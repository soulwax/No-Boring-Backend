package dev.oob.chaos.live;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.UUID;

import jakarta.annotation.PreDestroy;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Who is online across the whole room: each instance reports how many SSE clients it holds,
 * and the total counts every instance heard from recently. An instance that dies without
 * saying goodbye simply drops out of the sum after {@link #STALE}.
 */
@Repository
public class Presence {

	static final Duration STALE = Duration.ofSeconds(60);

	private final String instanceId = UUID.randomUUID().toString();

	private final JdbcClient jdbc;

	private final Clock clock;

	public Presence(JdbcClient jdbc, Clock clock) {
		this.jdbc = jdbc;
		this.clock = clock;
	}

	/** Upsert by hand: the one form H2 and Postgres agree on without dialect tricks. */
	public void report(int online) {
		var now = clock.instant().atOffset(ZoneOffset.UTC);
		int updated = jdbc.sql("update relay_instance set online = :online, seen_at = :now where instance_id = :id")
			.param("online", online)
			.param("now", now)
			.param("id", instanceId)
			.update();
		if (updated == 0) {
			jdbc.sql("insert into relay_instance (instance_id, online, seen_at) values (:id, :online, :now)")
				.param("id", instanceId)
				.param("online", online)
				.param("now", now)
				.update();
		}
	}

	public int total() {
		return jdbc.sql("select coalesce(sum(online), 0) from relay_instance where seen_at > :cutoff")
			.param("cutoff", clock.instant().minus(STALE).atOffset(ZoneOffset.UTC))
			.query(Integer.class)
			.single();
	}

	@PreDestroy
	void leave() {
		jdbc.sql("delete from relay_instance where instance_id = :id").param("id", instanceId).update();
	}

}
