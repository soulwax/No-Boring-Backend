package dev.oob.chaos.mutator;

import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import dev.oob.chaos.i18n.Localized;
import dev.oob.chaos.live.ChaosEvent;
import dev.oob.chaos.room.RoomState;
import dev.oob.chaos.room.RoomState.ActiveMutator;
import dev.oob.chaos.twist.Genre;
import dev.oob.chaos.twist.TwistEngine;

/**
 * Decides when the room's rules change. A mutator starts every so often on its own, and
 * immediately whenever a chaos wave hits; it lasts a fixed time, then calm returns.
 * The active mutator lives in {@link RoomState}, so every instance plays by the same rules,
 * and every instance's scheduler may try, but only one wins each start and expiry.
 */
@Component
@Transactional
public class MutatorDirector {

	private static final List<Genre> LOCKABLE_GENRES = Arrays.stream(Genre.values())
		.filter(g -> g != Genre.CLASSIC)
		.toList();

	private final ApplicationEventPublisher events;

	private final Clock clock;

	private final TwistEngine dice;

	private final Duration duration;

	private final RoomState room;

	public MutatorDirector(ApplicationEventPublisher events, Clock clock, TwistEngine dice,
			@Value("${chaos.mutator-duration}") Duration duration, RoomState room) {
		this.events = events;
		this.clock = clock;
		this.dice = dice;
		this.duration = duration;
		this.room = room;
	}

	@Transactional(readOnly = true)
	public Localized<MutatorState> state() {
		ActiveMutator a = current().orElse(null);
		return Localized.of(
				lang -> a == null ? MutatorState.NONE : MutatorState.of(a.mutator(), a.genre(), a.endsAt(), lang));
	}

	public Optional<Mutator> activeMutator() {
		return current().map(ActiveMutator::mutator);
	}

	public boolean isActive(Mutator mutator) {
		return activeMutator().orElse(null) == mutator;
	}

	public int hypeMultiplier() {
		return isActive(Mutator.DOUBLE_HYPE) ? 2 : 1;
	}

	public Optional<Genre> lockedGenre() {
		return current().map(ActiveMutator::genre);
	}

	public void startRandom() {
		start(dice.pickFrom(List.of(Mutator.values())), dice.pickFrom(LOCKABLE_GENRES));
	}

	/** Starts {@code mutator}, replacing any active one. {@code genre} only matters for a genre lock. */
	public void start(Mutator mutator, @Nullable Genre genre) {
		room.startMutator(mutator, lockGenre(mutator, genre), clock.instant().plus(duration));
		announce();
	}

	public void stop() {
		if (room.stopMutator()) {
			announce();
		}
	}

	@Scheduled(fixedRateString = "${chaos.mutator-interval}", initialDelayString = "${chaos.mutator-interval}")
	void scheduledStart() {
		Mutator mutator = dice.pickFrom(List.of(Mutator.values()));
		Genre genre = lockGenre(mutator, dice.pickFrom(LOCKABLE_GENRES));
		if (room.startMutatorIfCalm(mutator, genre, clock.instant().plus(duration), clock.instant())) {
			announce();
		}
	}

	@Scheduled(fixedRate = 1000)
	void expire() {
		if (room.expireMutator(clock.instant())) {
			announce();
		}
	}

	private Optional<ActiveMutator> current() {
		return room.mutator(clock.instant());
	}

	private static @Nullable Genre lockGenre(Mutator mutator, @Nullable Genre genre) {
		return mutator == Mutator.GENRE_LOCK ? genre : null;
	}

	private void announce() {
		Localized<MutatorState> state = state();
		events.publishEvent(ChaosEvent.changed(ChaosEvent.MUTATOR));
		events.publishEvent(new MutatorChanged(state));
	}

}
