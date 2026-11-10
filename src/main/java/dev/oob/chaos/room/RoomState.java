package dev.oob.chaos.room;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import dev.oob.chaos.mutator.Mutator;
import dev.oob.chaos.twist.Genre;

/**
 * The room-wide state every backend instance shares, kept in the single {@code room_state} row.
 * Each change is a conditional {@code UPDATE}: when several instances race (two schedulers
 * expiring the same mutator, two hypes crossing the wave threshold), exactly one sees a row
 * count of 1 and gets to announce it. No locks held, no leader election.
 */
@Repository
public class RoomState {

	public record ActiveMutator(Mutator mutator, @Nullable Genre genre, Instant endsAt) {
	}

	private final JdbcClient jdbc;

	public RoomState(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public Optional<ActiveMutator> mutator(Instant now) {
		return jdbc.sql("""
				select mutator, mutator_genre, mutator_ends_at from room_state
				where id = 1 and mutator is not null and mutator_ends_at > :now""")
			.param("now", at(now))
			.query((rs, row) -> new ActiveMutator(Mutator.valueOf(rs.getString(1)), genre(rs.getString(2)),
					rs.getObject(3, OffsetDateTime.class).toInstant()))
			.optional();
	}

	/** Replaces whatever is running. */
	public void startMutator(Mutator mutator, @Nullable Genre genre, Instant endsAt) {
		jdbc.sql("update room_state set mutator = :m, mutator_genre = :g, mutator_ends_at = :ends where id = 1")
			.param("m", mutator.name())
			.param("g", genre == null ? null : genre.name())
			.param("ends", at(endsAt))
			.update();
	}

	/** Starts {@code mutator} only if the room is calm; {@code true} if this call started it. */
	public boolean startMutatorIfCalm(Mutator mutator, @Nullable Genre genre, Instant endsAt, Instant now) {
		return jdbc.sql("""
				update room_state set mutator = :m, mutator_genre = :g, mutator_ends_at = :ends
				where id = 1 and (mutator is null or mutator_ends_at <= :now)""")
			.param("m", mutator.name())
			.param("g", genre == null ? null : genre.name())
			.param("ends", at(endsAt))
			.param("now", at(now))
			.update() == 1;
	}

	/** {@code true} if this call is the one that found the mutator expired and cleared it. */
	public boolean expireMutator(Instant now) {
		return jdbc.sql("""
				update room_state set mutator = null, mutator_genre = null, mutator_ends_at = null
				where id = 1 and mutator is not null and mutator_ends_at <= :now""")
			.param("now", at(now))
			.update() == 1;
	}

	public boolean stopMutator() {
		return jdbc.sql("""
				update room_state set mutator = null, mutator_genre = null, mutator_ends_at = null
				where id = 1 and mutator is not null""")
			.update() == 1;
	}

	/** {@code true} for the one caller that gets to unleash the wave. */
	public boolean disarmWave() {
		return jdbc.sql("update room_state set wave_armed = false where id = 1 and wave_armed").update() == 1;
	}

	public void rearmWave() {
		jdbc.sql("update room_state set wave_armed = true where id = 1 and not wave_armed").update();
	}

	/**
	 * Claims this entropy tick. Every instance schedules the decay, but only one per
	 * {@code interval} wins, so hype drains at the same speed however many instances run.
	 */
	public boolean claimDecay(Instant now, Duration interval) {
		return jdbc.sql("update room_state set decayed_at = :now where id = 1 and decayed_at <= :cutoff")
			.param("now", at(now))
			// A little slack, so jittery schedulers on one instance don't skip their own tick
			.param("cutoff", at(now.minus(interval.multipliedBy(3).dividedBy(4))))
			.update() == 1;
	}

	/** {@code SELECT … FOR UPDATE} on the room row: serializes one-off work across instances. */
	public void lock() {
		jdbc.sql("select id from room_state where id = 1 for update").query(Integer.class).single();
	}

	static OffsetDateTime at(Instant instant) {
		return instant.atOffset(ZoneOffset.UTC);
	}

	private static @Nullable Genre genre(@Nullable String name) {
		return name == null ? null : Genre.valueOf(name);
	}

}
