package dev.oob.chaos.achievements;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import dev.oob.chaos.board.PlayerAction;
import dev.oob.chaos.board.PlayerAction.Kind;
import dev.oob.chaos.board.ThingView;
import dev.oob.chaos.board.Tier;
import dev.oob.chaos.board.WaveTriggered;
import dev.oob.chaos.i18n.Lang;
import dev.oob.chaos.live.ChaosEvent;
import dev.oob.chaos.mutator.Mutator;
import dev.oob.chaos.player.Player;
import dev.oob.chaos.support.TestClock;
import dev.oob.chaos.twist.Genre;

/** Against the real schema (Flyway on H2 in PostgreSQL mode), so the row lock and upsert run too. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ AchievementTracker.class, AchievementTrackerTest.Clocks.class })
@RecordApplicationEvents
class AchievementTrackerTest {

	@TestConfiguration
	static class Clocks {

		/** Wins over the application's system clock */
		@Bean
		@Primary
		TestClock testClock() {
			return new TestClock();
		}

	}

	@Autowired
	AchievementTracker tracker;

	@Autowired
	ApplicationEvents events;

	@Autowired
	TestClock clock;

	@Autowired
	PlayerProgressRepository progress;

	/** A fresh player per test: the in-memory database outlives a single test */
	private final Player ada = new Player("ada-" + UUID.randomUUID(), "Ada");

	@Test
	void unlocksEachAchievementExactlyOnceAndBroadcastsIt() {
		tracker.on(action(ada, Kind.SUBMIT, false));
		tracker.on(action(ada, Kind.SUBMIT, false));

		assertThat(unlockedCodes()).containsExactly("BOREDOM_SLAYER");
		assertThat(unlocked(Lang.EN).getFirst().playerTag()).isEqualTo(ada.tag()).isNotEqualTo(ada.id());
	}

	@Test
	void broadcastsEveryUnlockInEveryLanguage() {
		tracker.on(action(ada, Kind.SUBMIT, false));

		assertThat(unlocked(Lang.DE).getFirst().title()).isEqualTo("Langeweile-Bezwinger");
		assertThat(unlocked(Lang.EN).getFirst().title()).isEqualTo("Boredom Slayer");
		assertThat(tracker.forPlayer(ada, Lang.DE).getFirst().description())
			.isEqualTo("Reiche dein erstes langweiliges Ding ein");
	}

	@Test
	void hypingYourOwnThingAndDoingEverythingUnlocksComboAchievements() {
		tracker.on(action(ada, Kind.SUBMIT, false));
		tracker.on(action(ada, Kind.HYPE, true));
		tracker.on(action(ada, Kind.REROLL, false));
		tracker.on(new WaveTriggered(ada));

		assertThat(unlockedCodes()).containsExactly("BOREDOM_SLAYER", "SHAMELESS", "RENAISSANCE", "WAVE_RIDER");
		assertThat(tracker.forPlayer(ada, Lang.EN)).filteredOn(AchievementView::unlocked).hasSize(4);
	}

	@Test
	void evolutionGenresAndMutatorsHaveTheirOwnTrophies() {
		for (Genre genre : List.of(Genre.NOIR, Genre.HEIST, Genre.NOIR, Genre.MEDIEVAL, Genre.SPACE_OPERA)) {
			tracker.on(new PlayerAction(ada, Kind.SUBMIT, thing(genre), false, null, null));
		}
		tracker.on(new PlayerAction(ada, Kind.HYPE, thing(Genre.NOIR), false, Tier.MYTHIC, Mutator.DOUBLE_HYPE));

		assertThat(unlockedCodes()).contains("GENRE_HOPPER", "MYTHMAKER", "STORM_CHASER");
	}

	@Test
	void nightOwlsAreRecognized() {
		clock.now = Instant.parse("2026-11-10T03:00:00Z");

		tracker.on(action(ada, Kind.HYPE, false));

		assertThat(unlockedCodes()).containsExactly("INSOMNIAC");
	}

	@Test
	void progressIsStoredWithTheMomentEachTrophyWasEarned() {
		Instant earned = clock.now;
		tracker.on(action(ada, Kind.SUBMIT, false));
		clock.now = earned.plusSeconds(60);
		progress.flush();

		TrophyShelf shelf = tracker.shelf(ada, Lang.EN);

		assertThat(shelf.unlocked()).isEqualTo(1);
		assertThat(shelf.total()).isEqualTo(Achievement.values().length);
		assertThat(shelf.trophies()).filteredOn(t -> t.code().equals("BOREDOM_SLAYER"))
			.singleElement()
			.extracting(Trophy::unlockedAt)
			.isEqualTo(earned);
	}

	@Test
	void untrackedPlayersEarnNothing() {
		tracker.on(action(Player.ANONYMOUS, Kind.SUBMIT, false));

		assertThat(events.stream(ChaosEvent.class)).isEmpty();
		assertThat(tracker.forPlayer(Player.ANONYMOUS, Lang.EN)).noneMatch(AchievementView::unlocked);
	}

	private static PlayerAction action(Player player, Kind kind, boolean ownThing) {
		return new PlayerAction(player, kind, thing(Genre.CLASSIC), ownThing, null, null);
	}

	private static ThingView thing(Genre genre) {
		return new ThingView(1L, "x", "X", "Ada", genre, 0, 0, Tier.BORING, Tier.BORING, 6, List.of(), Instant.EPOCH);
	}

	private List<AchievementUnlocked> unlocked(Lang lang) {
		return events.stream(ChaosEvent.class).map(e -> (AchievementUnlocked) e.payloadIn(lang)).toList();
	}

	private List<String> unlockedCodes() {
		return unlocked(Lang.EN).stream().map(AchievementUnlocked::code).toList();
	}

}
