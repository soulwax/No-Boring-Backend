package dev.oob.chaos.mutator;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dev.oob.chaos.live.ChaosEvent;
import dev.oob.chaos.twist.Genre;
import dev.oob.chaos.twist.TwistEngine;

/**
 * Decides when the room's rules change. A mutator starts every so often on its own, and
 * immediately whenever a chaos wave hits; it lasts a fixed time, then calm returns.
 */
@Component
public class MutatorDirector {

	private record Active(Mutator mutator, Genre genre, Instant endsAt) {
	}

	private static final List<Genre> LOCKABLE_GENRES = Arrays.stream(Genre.values())
		.filter(g -> g != Genre.CLASSIC)
		.toList();

	private final AtomicReference<Active> active = new AtomicReference<>();

	private final ApplicationEventPublisher events;

	private final Clock clock;

	private final TwistEngine dice;

	private final Duration duration;

	public MutatorDirector(ApplicationEventPublisher events, Clock clock, TwistEngine dice,
			@Value("${chaos.mutator-duration}") Duration duration) {
		this.events = events;
		this.clock = clock;
		this.dice = dice;
		this.duration = duration;
	}

	public MutatorState state() {
		return current().map(a -> MutatorState.of(a.mutator(), a.genre(), a.endsAt())).orElse(MutatorState.NONE);
	}

	public Optional<Mutator> activeMutator() {
		return current().map(Active::mutator);
	}

	public boolean isActive(Mutator mutator) {
		return activeMutator().orElse(null) == mutator;
	}

	public int hypeMultiplier() {
		return isActive(Mutator.DOUBLE_HYPE) ? 2 : 1;
	}

	public Optional<Genre> lockedGenre() {
		return current().map(Active::genre);
	}

	public void startRandom() {
		start(dice.pickFrom(List.of(Mutator.values())), dice.pickFrom(LOCKABLE_GENRES));
	}

	/** Starts {@code mutator}, replacing any active one. {@code genre} only matters for a genre lock. */
	public void start(Mutator mutator, Genre genre) {
		active.set(new Active(mutator, mutator == Mutator.GENRE_LOCK ? genre : null, clock.instant().plus(duration)));
		announce();
	}

	public void stop() {
		if (active.getAndSet(null) != null) {
			announce();
		}
	}

	@Scheduled(fixedRateString = "${chaos.mutator-interval}", initialDelayString = "${chaos.mutator-interval}")
	void scheduledStart() {
		if (current().isEmpty()) {
			startRandom();
		}
	}

	@Scheduled(fixedRate = 1000)
	void expire() {
		Active a = active.get();
		if (a != null && !a.endsAt().isAfter(clock.instant()) && active.compareAndSet(a, null)) {
			announce();
		}
	}

	private Optional<Active> current() {
		Active a = active.get();
		return (a != null && a.endsAt().isAfter(clock.instant())) ? Optional.of(a) : Optional.empty();
	}

	private void announce() {
		MutatorState state = state();
		events.publishEvent(new ChaosEvent("mutator", state));
		events.publishEvent(new MutatorChanged(state));
	}

}
