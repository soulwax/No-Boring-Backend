package dev.oob.chaos.live;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;

import org.jspecify.annotations.Nullable;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

/**
 * Holds this instance's {@code LISTEN chaos} connection on a virtual thread. LISTEN needs a
 * session of its own, so it uses the direct (unpooled) URL; PgBouncer in transaction mode would
 * hand the session to someone else. On any failure it reconnects with exponential backoff and
 * resyncs, so a dropped connection costs a pause, never a lost event.
 */
@Component
@ConditionalOnProperty(name = "chaos.relay.mode", havingValue = "postgres")
class PostgresListener implements SmartLifecycle {

	private static final Logger log = LoggerFactory.getLogger(PostgresListener.class);

	private static final int POLL_MILLIS = 10_000;

	private static final Duration MAX_BACKOFF = Duration.ofSeconds(30);

	private final String url;

	private final String username;

	private final String password;

	private final ChaosFanout fanout;

	private volatile boolean running;

	private volatile boolean listening;

	private volatile @Nullable Thread thread;

	private volatile @Nullable Connection connection;

	PostgresListener(@Value("${chaos.database.direct-url}") String url,
			@Value("${chaos.database.direct-username}") String username,
			@Value("${chaos.database.direct-password}") String password, ChaosFanout fanout) {
		this.url = url;
		this.username = username;
		this.password = password;
		this.fanout = fanout;
	}

	/** For the health indicator */
	boolean isListening() {
		return listening;
	}

	@Override
	public void start() {
		running = true;
		thread = Thread.ofVirtual().name("chaos-listen").start(this::listenForever);
	}

	@Override
	public void stop() {
		running = false;
		closeQuietly(connection);
		Thread t = thread;
		if (t != null) {
			t.interrupt();
		}
	}

	@Override
	public boolean isRunning() {
		return running;
	}

	private void listenForever() {
		Duration backoff = Duration.ofSeconds(1);
		while (running) {
			try (Connection c = DriverManager.getConnection(url, username, password)) {
				connection = c;
				try (Statement s = c.createStatement()) {
					s.execute("LISTEN " + PostgresTransport.CHANNEL);
				}
				listening = true;
				backoff = Duration.ofSeconds(1);
				log.info("Listening for room events on {}", url.replaceAll("\\?.*", ""));
				fanout.resync();
				poll(c.unwrap(PGConnection.class), c);
			}
			catch (SQLException | RuntimeException ex) {
				if (!running) {
					return;
				}
				log.warn("Room event listener lost its connection, retrying in {}s: {}", backoff.toSeconds(),
						ex.getMessage());
			}
			finally {
				listening = false;
				connection = null;
			}
			sleep(backoff);
			backoff = backoff.multipliedBy(2).compareTo(MAX_BACKOFF) > 0 ? MAX_BACKOFF : backoff.multipliedBy(2);
		}
	}

	private void poll(PGConnection pg, Connection c) throws SQLException {
		while (running) {
			PGNotification[] notifications = pg.getNotifications(POLL_MILLIS);
			if (notifications == null || notifications.length == 0) {
				// Quiet room: prove the connection is still alive, or fail into a reconnect
				try (Statement s = c.createStatement()) {
					s.execute("select 1");
				}
				continue;
			}
			for (PGNotification n : notifications) {
				String message = n.getParameter();
				int colon = message.indexOf(':');
				try {
					fanout.deliver(colon < 0 ? message : message.substring(0, colon),
							colon < 0 ? null : Long.valueOf(message.substring(colon + 1)));
				}
				catch (RuntimeException ex) {
					// One undeliverable event must not take the listener down with it
					log.warn("Could not deliver room event {}: {}", message, ex.getMessage());
				}
			}
		}
	}

	private void sleep(Duration duration) {
		try {
			Thread.sleep(duration);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			running = false;
		}
	}

	private static void closeQuietly(@Nullable Connection c) {
		if (c != null) {
			try {
				c.close();
			}
			catch (SQLException ex) {
				// closing anyway
			}
		}
	}

}
