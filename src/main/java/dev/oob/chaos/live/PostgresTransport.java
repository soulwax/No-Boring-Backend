package dev.oob.chaos.live;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Multi-instance delivery: {@code pg_notify} inside the same transaction. Postgres queues the
 * notification and delivers it to every listening instance (this one included) only if the
 * transaction commits, in commit order. A rollback takes the outbox row and the notification
 * with it. NOTIFY travels fine through the transaction pooler; only LISTEN needs a direct
 * connection ({@link PostgresListener}).
 */
@Component
@ConditionalOnProperty(name = "chaos.relay.mode", havingValue = "postgres")
class PostgresTransport implements RelayTransport {

	static final String CHANNEL = "chaos";

	private final JdbcClient jdbc;

	PostgresTransport(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public void deliver(String type, @Nullable Long id, @Nullable Object payload) {
		jdbc.sql("select pg_notify(:channel, :message)")
			.param("channel", CHANNEL)
			.param("message", id == null ? type : type + ":" + id)
			.query()
			.listOfRows();
	}

}
