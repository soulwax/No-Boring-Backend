package dev.oob.chaos.live;

import java.time.Clock;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;

/**
 * The receiving end of {@link PostgresTransport}: turns one notification into SSE messages for
 * this instance's clients. Stored events are read back from the outbox; derived ones are
 * rendered from this instance's own view of the database.
 */
@Component
@ConditionalOnProperty(name = "chaos.relay.mode", havingValue = "postgres")
public class ChaosFanout {

	private static final List<String> STORED = List.of(ChaosEvent.THING, ChaosEvent.ACTIVITY,
			ChaosEvent.ACHIEVEMENT, ChaosEvent.WAVE);

	/** A reconnect replays at most this much; anything older is covered by the fresh snapshots */
	private static final int MAX_REPLAY = 500;

	/**
	 * Ids are handed out at insert but delivered at commit, so they arrive slightly out of order.
	 * A replay starts this far behind the newest id seen, and {@link #delivered} filters repeats.
	 */
	private static final int REPLAY_OVERLAP = 100;

	private static final int REMEMBERED = 2_000;

	private final ChaosLog log;

	private final ChaosBroadcaster broadcaster;

	private final RoomSnapshot snapshot;

	private final Clock clock;

	private final Timer lag;

	/** Newest outbox row this instance has delivered; {@code -1} until the first connect */
	private final AtomicLong lastSeen = new AtomicLong(-1);

	/** The most recent ids delivered, so a replay racing a live notification can't double a toast */
	private final Set<Long> delivered = Collections.newSetFromMap(Collections.synchronizedMap(
			new LinkedHashMap<Long, Boolean>() {
				@Override
				protected boolean removeEldestEntry(Map.Entry<Long, Boolean> eldest) {
					return size() > REMEMBERED;
				}
			}));

	ChaosFanout(ChaosLog log, ChaosBroadcaster broadcaster, RoomSnapshot snapshot, Clock clock, MeterRegistry meters) {
		this.log = log;
		this.broadcaster = broadcaster;
		this.snapshot = snapshot;
		this.clock = clock;
		this.lag = Timer.builder("chaos.relay.lag")
			.description("Commit on any instance to delivery on this one")
			.publishPercentiles(0.5, 0.95, 0.99)
			.register(meters);
	}

	/** Transient database trouble (a Neon compute waking up, a pooler hiccup) is retried, not dropped. */
	@Retryable(includes = TransientDataAccessException.class, maxRetries = 4, delay = 100, multiplier = 2,
			jitter = 50)
	public void deliver(String type, @Nullable Long id) {
		if (id == null) {
			broadcaster.broadcast(type, null, snapshot.of(type));
			return;
		}
		log.find(id).ifPresent(this::send);
	}

	/**
	 * After (re)connecting the listener: replay what was missed while deaf, then refresh every
	 * snapshot, since derived changes leave no trace in the outbox.
	 */
	@Retryable(includes = TransientDataAccessException.class, maxRetries = 4, delay = 100, multiplier = 2,
			jitter = 50)
	public void resync() {
		long from = lastSeen.get();
		if (from < 0) {
			lastSeen.compareAndSet(-1, log.lastId());
		}
		else {
			log.since(Math.max(0, from - REPLAY_OVERLAP), STORED, MAX_REPLAY).forEach(this::send);
		}
		ChaosEvent.DERIVED.forEach(type -> broadcaster.broadcast(type, null, snapshot.of(type)));
	}

	private void send(ChaosLog.Entry entry) {
		if (!delivered.add(entry.id())) {
			return;
		}
		lastSeen.accumulateAndGet(entry.id(), Math::max);
		lag.record(Duration.between(entry.at(), clock.instant()));
		broadcaster.broadcast(entry.type(), entry.id(), entry);
	}

}
