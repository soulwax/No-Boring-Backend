package dev.oob.chaos.mutator;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.oob.chaos.live.ChaosEvent;
import dev.oob.chaos.twist.Genre;
import dev.oob.chaos.twist.TwistEngine;

class MutatorDirectorTest {

	/** A clock the test can move forward. */
	static final class TestClock extends Clock {

		Instant now = Instant.parse("2026-11-10T12:00:00Z");

		@Override
		public ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return this;
		}

		@Override
		public Instant instant() {
			return now;
		}

	}

	private final List<Object> published = new ArrayList<>();

	private final TestClock clock = new TestClock();

	private final MutatorDirector director = new MutatorDirector(published::add, clock, new TwistEngine(),
			Duration.ofSeconds(45));

	@Test
	void doubleHypeDoublesUntilItExpires() {
		director.start(Mutator.DOUBLE_HYPE, Genre.NOIR);

		assertThat(director.hypeMultiplier()).isEqualTo(2);
		assertThat(director.lockedGenre()).as("genre only applies to a lock").isEmpty();
		assertThat(director.state().endsAt()).isEqualTo(clock.now.plusSeconds(45));

		clock.now = clock.now.plusSeconds(46);
		director.expire();

		assertThat(director.hypeMultiplier()).isEqualTo(1);
		assertThat(mutatorStates()).extracting(MutatorState::active).containsExactly(true, false);
	}

	@Test
	void genreLockNamesItsGenre() {
		director.start(Mutator.GENRE_LOCK, Genre.HEIST);

		assertThat(director.lockedGenre()).contains(Genre.HEIST);
		assertThat(director.state().title()).isEqualTo("Genre Lock: Heist");
	}

	@Test
	void scheduledStartLeavesARunningMutatorAlone() {
		director.start(Mutator.FROZEN_TIME, null);
		director.scheduledStart();

		assertThat(director.isActive(Mutator.FROZEN_TIME)).isTrue();
		assertThat(mutatorStates()).hasSize(1);
	}

	private List<MutatorState> mutatorStates() {
		return published.stream()
			.filter(ChaosEvent.class::isInstance)
			.map(e -> (MutatorState) ((ChaosEvent) e).payload())
			.toList();
	}

}
