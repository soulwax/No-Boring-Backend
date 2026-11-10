package dev.oob.chaos.mutator;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;

import dev.oob.chaos.i18n.Lang;
import dev.oob.chaos.room.RoomState;
import dev.oob.chaos.support.TestClock;
import dev.oob.chaos.support.TestDatabase;
import dev.oob.chaos.twist.Genre;
import dev.oob.chaos.twist.TwistEngine;

class MutatorDirectorTest {

	private final List<Object> published = new ArrayList<>();

	private final TestClock clock = new TestClock();

	private final RoomState room = new RoomState(JdbcClient.create(TestDatabase.migrated()));

	private final MutatorDirector director = director();

	@Test
	void doubleHypeDoublesUntilItExpires() {
		director.start(Mutator.DOUBLE_HYPE, Genre.NOIR);

		assertThat(director.hypeMultiplier()).isEqualTo(2);
		assertThat(director.lockedGenre()).as("genre only applies to a lock").isEmpty();
		assertThat(director.state().in(Lang.EN).endsAt()).isEqualTo(clock.now.plusSeconds(45));

		clock.now = clock.now.plusSeconds(46);
		director.expire();

		assertThat(director.hypeMultiplier()).isEqualTo(1);
		assertThat(changes()).extracting(MutatorState::active).containsExactly(true, false);
	}

	@Test
	void genreLockNamesItsGenre() {
		director.start(Mutator.GENRE_LOCK, Genre.HEIST);

		assertThat(director.lockedGenre()).contains(Genre.HEIST);
		assertThat(director.state().in(Lang.EN).title()).isEqualTo("Genre Lock: Heist");
		assertThat(director.state().in(Lang.DE).title()).isEqualTo("Genre-Sperre: Raubzug");
	}

	@Test
	void scheduledStartLeavesARunningMutatorAlone() {
		director.start(Mutator.FROZEN_TIME, null);
		director.scheduledStart();

		assertThat(director.isActive(Mutator.FROZEN_TIME)).isTrue();
		assertThat(changes()).hasSize(1);
	}

	@Test
	void instancesShareOneMutatorAndOnlyOneAnnouncesItsEnd() {
		MutatorDirector other = director();
		director.start(Mutator.TWIST_TORNADO, null);

		assertThat(other.isActive(Mutator.TWIST_TORNADO)).as("seen by the other instance").isTrue();

		clock.now = clock.now.plusSeconds(46);
		other.expire();
		director.expire();
		other.scheduledStart();
		director.scheduledStart();

		// One start, one expiry, then exactly one of the two schedulers wins the calm room
		assertThat(changes()).extracting(MutatorState::active).containsExactly(true, false, true);
	}

	private MutatorDirector director() {
		return new MutatorDirector(published::add, clock, new TwistEngine(), Duration.ofSeconds(45), room);
	}

	private List<MutatorState> changes() {
		return published.stream()
			.filter(MutatorChanged.class::isInstance)
			.map(e -> ((MutatorChanged) e).state().in(Lang.EN))
			.toList();
	}

}
