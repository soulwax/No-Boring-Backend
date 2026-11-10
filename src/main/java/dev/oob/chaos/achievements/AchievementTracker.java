package dev.oob.chaos.achievements;

import java.time.Clock;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import dev.oob.chaos.board.PlayerAction;
import dev.oob.chaos.board.Tier;
import dev.oob.chaos.board.WaveTriggered;
import dev.oob.chaos.i18n.Lang;
import dev.oob.chaos.i18n.Localized;
import dev.oob.chaos.live.ChaosEvent;
import dev.oob.chaos.player.Player;

/**
 * Listens to the board's domain events; the board knows nothing about achievements.
 * Runs inside the board's transaction: progress is saved with the hype that earned it, and
 * the "achievement" broadcast goes out only if both commit.
 */
@Component
public class AchievementTracker {

	private final PlayerProgressRepository progress;

	private final ApplicationEventPublisher events;

	private final Clock clock;

	AchievementTracker(PlayerProgressRepository progress, ApplicationEventPublisher events, Clock clock) {
		this.progress = progress;
		this.events = events;
		this.clock = clock;
	}

	@EventListener
	@Transactional
	void on(PlayerAction action) {
		boolean night = LocalTime.now(clock).getHour() < 5;
		record(action.player(), p -> {
			switch (action.kind()) {
				case SUBMIT -> {
					p.submits++;
					p.addGenre(action.thing().genre());
				}
				case HYPE -> {
					p.hypes++;
					if (action.ownThing()) {
						p.selfHypes++;
					}
					if (action.promotedTo() == Tier.MYTHIC) {
						p.mythics++;
					}
					if (action.mutator() != null) {
						p.mutatorHypes++;
					}
				}
				case REROLL -> p.rerolls++;
			}
			p.nightOwl |= night;
		});
	}

	@EventListener
	@Transactional
	void on(WaveTriggered wave) {
		record(wave.player(), p -> p.triggeredWave = true);
	}

	/** API version 1: every achievement, unlocked or not */
	@Transactional(readOnly = true)
	public List<AchievementView> forPlayer(Player player, Lang lang) {
		PlayerProgress mine = mine(player);
		return Arrays.stream(Achievement.values())
			.map(a -> AchievementView.of(a, mine != null && mine.has(a), lang))
			.toList();
	}

	/** API version 2: the same, with unlock times and the counting done */
	@Transactional(readOnly = true)
	public TrophyShelf shelf(Player player, Lang lang) {
		PlayerProgress mine = mine(player);
		List<Trophy> trophies = Arrays.stream(Achievement.values())
			.map(a -> Trophy.of(a, mine == null ? null : mine.unlockedAt(a), lang))
			.toList();
		int unlocked = (int) trophies.stream().filter(t -> t.unlockedAt() != null).count();
		return new TrophyShelf(unlocked, trophies.size(), trophies);
	}

	/**
	 * Moves a guest's progress onto a signed-in player, then announces whatever the combined
	 * counters newly earn (three submissions as a guest plus two signed in make Content Creator).
	 * Joins the caller's transaction.
	 */
	@Transactional
	public void adopt(String guestId, Player account) {
		PlayerProgress guest = progress.findById(guestId).orElse(null);
		if (guest == null || guestId.equals(account.id())) {
			return;
		}
		record(account, p -> p.absorb(guest));
		progress.delete(guest);
	}

	private @Nullable PlayerProgress mine(Player player) {
		return player.isTracked() ? progress.findById(player.id()).orElse(null) : null;
	}

	private void record(Player player, Consumer<PlayerProgress> change) {
		if (!player.isTracked()) {
			return;
		}
		progress.ensureExists(player.id());
		PlayerProgress mine = progress.lock(player.id()).orElseThrow();
		mine.record(change, clock.instant())
			.forEach(a -> events.publishEvent(new ChaosEvent(ChaosEvent.ACHIEVEMENT,
					Localized.of(lang -> AchievementUnlocked.of(player, a, lang)))));
	}

}
