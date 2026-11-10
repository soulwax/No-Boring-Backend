package dev.oob.chaos.achievements;

import java.time.Clock;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import dev.oob.chaos.board.PlayerAction;
import dev.oob.chaos.board.WaveTriggered;
import dev.oob.chaos.live.ChaosEvent;
import dev.oob.chaos.player.Player;

/**
 * Listens to the board's domain events; the board knows nothing about achievements.
 * Runs inside the board's transaction, so the "achievement" broadcast it publishes is
 * held back until commit like every other {@link ChaosEvent}.
 * <p>
 * Progress lives in memory: restart the server and everyone starts over.
 */
@Component
public class AchievementTracker {

	private final Map<String, PlayerProgress> progress = new ConcurrentHashMap<>();

	private final ApplicationEventPublisher events;

	private final Clock clock;

	public AchievementTracker(ApplicationEventPublisher events, Clock clock) {
		this.events = events;
		this.clock = clock;
	}

	@EventListener
	void on(PlayerAction action) {
		boolean night = LocalTime.now(clock).getHour() < 5;
		record(action.player(), p -> {
			switch (action.kind()) {
				case SUBMIT -> p.submits++;
				case HYPE -> {
					p.hypes++;
					if (action.ownThing()) {
						p.selfHypes++;
					}
				}
				case REROLL -> p.rerolls++;
			}
			p.nightOwl |= night;
		});
	}

	@EventListener
	void on(WaveTriggered wave) {
		record(wave.player(), p -> p.triggeredWave = true);
	}

	public List<AchievementView> forPlayer(Player player) {
		PlayerProgress mine = player.isTracked() ? progress.get(player.id()) : null;
		return Arrays.stream(Achievement.values())
			.map(a -> AchievementView.of(a, mine != null && mine.has(a)))
			.toList();
	}

	private void record(Player player, Consumer<PlayerProgress> change) {
		if (!player.isTracked()) {
			return;
		}
		progress.computeIfAbsent(player.id(), id -> new PlayerProgress())
			.record(change)
			.forEach(a -> events.publishEvent(new ChaosEvent("achievement", AchievementUnlocked.of(player, a))));
	}

}
