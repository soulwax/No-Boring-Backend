package dev.oob.chaos.achievements;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.oob.chaos.board.PlayerAction;
import dev.oob.chaos.board.PlayerAction.Kind;
import dev.oob.chaos.board.WaveTriggered;
import dev.oob.chaos.live.ChaosEvent;
import dev.oob.chaos.player.Player;

class AchievementTrackerTest {

	private static final Clock NOON = Clock.fixed(Instant.parse("2026-11-10T12:00:00Z"), ZoneOffset.UTC);

	private static final Clock THREE_AM = Clock.fixed(Instant.parse("2026-11-10T03:00:00Z"), ZoneOffset.UTC);

	private final List<Object> published = new ArrayList<>();

	private final Player ada = new Player("ada-1234-5678", "Ada");

	@Test
	void unlocksEachAchievementExactlyOnceAndBroadcastsIt() {
		AchievementTracker tracker = new AchievementTracker(published::add, NOON);

		tracker.on(new PlayerAction(ada, Kind.SUBMIT, false));
		tracker.on(new PlayerAction(ada, Kind.SUBMIT, false));

		assertThat(unlockedCodes()).containsExactly("BOREDOM_SLAYER");
		assertThat(((AchievementUnlocked) ((ChaosEvent) published.getFirst()).payload()).playerTag())
			.isEqualTo(ada.tag())
			.isNotEqualTo(ada.id());
	}

	@Test
	void hypingYourOwnThingAndDoingEverythingUnlocksComboAchievements() {
		AchievementTracker tracker = new AchievementTracker(published::add, NOON);

		tracker.on(new PlayerAction(ada, Kind.SUBMIT, false));
		tracker.on(new PlayerAction(ada, Kind.HYPE, true));
		tracker.on(new PlayerAction(ada, Kind.REROLL, false));
		tracker.on(new WaveTriggered(ada));

		assertThat(unlockedCodes()).containsExactly("BOREDOM_SLAYER", "SHAMELESS", "RENAISSANCE", "WAVE_RIDER");
		assertThat(tracker.forPlayer(ada)).filteredOn(AchievementView::unlocked).hasSize(4);
	}

	@Test
	void nightOwlsAreRecognized() {
		AchievementTracker tracker = new AchievementTracker(published::add, THREE_AM);

		tracker.on(new PlayerAction(ada, Kind.HYPE, false));

		assertThat(unlockedCodes()).containsExactly("INSOMNIAC");
	}

	@Test
	void untrackedPlayersEarnNothing() {
		AchievementTracker tracker = new AchievementTracker(published::add, THREE_AM);

		tracker.on(new PlayerAction(Player.ANONYMOUS, Kind.SUBMIT, false));

		assertThat(published).isEmpty();
		assertThat(tracker.forPlayer(Player.ANONYMOUS)).noneMatch(AchievementView::unlocked);
	}

	private List<String> unlockedCodes() {
		return published.stream()
			.map(e -> ((AchievementUnlocked) ((ChaosEvent) e).payload()).code())
			.toList();
	}

}
