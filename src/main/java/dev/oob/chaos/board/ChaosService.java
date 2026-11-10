package dev.oob.chaos.board;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.resilience.annotation.ConcurrencyLimit;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.oob.chaos.i18n.Lang;
import dev.oob.chaos.i18n.Localized;
import dev.oob.chaos.live.ChaosEvent;
import dev.oob.chaos.mutator.Mutator;
import dev.oob.chaos.mutator.MutatorDirector;
import dev.oob.chaos.player.Player;
import dev.oob.chaos.room.RoomState;
import dev.oob.chaos.twist.Genre;
import dev.oob.chaos.twist.TwistEngine;

/**
 * The board's rules. Mutations run under two Spring Framework 7 resilience annotations, both
 * wrapped around the transaction: {@code @ConcurrencyLimit}, because on virtual threads nothing
 * else stops a burst of hypes from queueing on the connection pool, and {@code @Retryable},
 * because a transient failure rolls back the change together with its outbox events, so a
 * clean retry is always safe.
 */
@Service
@Transactional
public class ChaosService {

	static final int HYPE_PER_CLICK = 3;

	static final int DECAY_PER_TICK = 1;

	/** Crossing this meter level unleashes a chaos wave on every screen. */
	static final int WAVE_LEVEL = 85;

	/** The meter must calm down below this before another wave can happen. */
	static final int WAVE_REARM_LEVEL = 50;

	private static final List<Genre> GENRES = List.of(Genre.values());

	private final BoringThingRepository things;

	private final TwistEngine twistEngine;

	private final MutatorDirector mutators;

	private final ApplicationEventPublisher events;

	private final RoomState room;

	private final Clock clock;

	private final Duration decayInterval;

	public ChaosService(BoringThingRepository things, TwistEngine twistEngine, MutatorDirector mutators,
			ApplicationEventPublisher events, RoomState room, Clock clock,
			@Value("${chaos.decay-interval}") Duration decayInterval) {
		this.things = things;
		this.twistEngine = twistEngine;
		this.mutators = mutators;
		this.events = events;
		this.room = room;
		this.clock = clock;
		this.decayInterval = decayInterval;
	}

	@Transactional(readOnly = true)
	public Localized<List<ThingView>> board() {
		List<BoringThing> top = things.findTop50ByOrderByHypeDescCreatedAtDesc();
		return Localized.of(lang -> top.stream().map(thing -> ThingView.of(thing, lang)).toList());
	}

	@Transactional(readOnly = true)
	public Localized<ChaosMeter> meter() {
		long totalHype = things.totalHype();
		long count = things.count();
		return Localized.of(lang -> ChaosMeter.from(totalHype, count, lang));
	}

	/** @param requested the genre the player picked, or {@code null} for a surprise */
	@ConcurrencyLimit(limitString = "${chaos.mutation-concurrency}")
	@Retryable(includes = TransientDataAccessException.class, maxRetries = 2, delay = 50, jitter = 25)
	public ThingView submit(String original, @Nullable String author, @Nullable Genre requested, Player player,
			Lang lang) {
		String name = (author == null || author.isBlank()) ? player.name() : author.strip();
		Genre genre = mutators.lockedGenre()
			.orElseGet(() -> requested != null ? requested : twistEngine.pickFrom(GENRES));
		BoringThing thing = things.save(
				new BoringThing(original.strip(), twistEngine.roll(genre), name, genre, player.id()));
		return changed(thing, player, lang, PlayerAction.Kind.SUBMIT, null);
	}

	@ConcurrencyLimit(limitString = "${chaos.mutation-concurrency}")
	@Retryable(includes = TransientDataAccessException.class, maxRetries = 2, delay = 50, jitter = 25)
	public ThingView hype(long id, Player player, Lang lang) {
		BoringThing thing = find(id);
		thing.addHype(HYPE_PER_CLICK * mutators.hypeMultiplier());
		if (mutators.isActive(Mutator.TWIST_TORNADO)) {
			thing.retwist(twistEngine.roll(genreFor(thing)));
		}
		Tier promoted = thing.evolve(this::loreFor).orElse(null);
		return changed(thing, player, lang, PlayerAction.Kind.HYPE, promoted);
	}

	@ConcurrencyLimit(limitString = "${chaos.mutation-concurrency}")
	@Retryable(includes = TransientDataAccessException.class, maxRetries = 2, delay = 50, jitter = 25)
	public ThingView reroll(long id, Player player, Lang lang) {
		BoringThing thing = find(id);
		thing.retwist(twistEngine.roll(genreFor(thing)));
		return changed(thing, player, lang, PlayerAction.Kind.REROLL, null);
	}

	/**
	 * Hype fades on its own, so the board is never the same twice. Unless time is frozen. Every
	 * instance schedules it; {@link RoomState#claimDecay} lets exactly one of them run each tick.
	 */
	@Scheduled(fixedRateString = "${chaos.decay-interval}", initialDelayString = "${chaos.decay-interval}")
	public void decay() {
		if (mutators.isActive(Mutator.FROZEN_TIME) || !room.claimDecay(clock.instant(), decayInterval)) {
			return;
		}
		if (things.decayHype(DECAY_PER_TICK) > 0) {
			events.publishEvent(ChaosEvent.changed(ChaosEvent.BOARD));
			events.publishEvent(ChaosEvent.changed(ChaosEvent.METER));
			if (meter().in(Lang.DEFAULT).level() < WAVE_REARM_LEVEL) {
				room.rearmWave();
			}
		}
	}

	/** Re-rolls stay in the thing's own genre, unless a genre lock says otherwise. */
	private Genre genreFor(BoringThing thing) {
		return mutators.lockedGenre().orElse(thing.getGenre());
	}

	private Lore loreFor(Tier tier) {
		return new Lore(tier, twistEngine.pickIndex(tier.escalations()));
	}

	/** @param lang the language the acting player reads, for the view they get back */
	private ThingView changed(BoringThing thing, Player player, Lang lang, PlayerAction.Kind kind,
			@Nullable Tier promoted) {
		things.flush();
		Localized<ThingView> view = Localized.of(l -> ThingView.of(thing, l));
		int level = meter().in(Lang.DEFAULT).level();
		events.publishEvent(ChaosEvent.thing(view));
		events.publishEvent(ChaosEvent.changed(ChaosEvent.METER));
		events.publishEvent(new PlayerAction(player, kind, view.in(lang), thing.isOwnedBy(player), promoted,
				mutators.activeMutator().orElse(null)));

		// Whichever instance flips the shared flag first unleashes the wave; everyone else just hyped
		if (level >= WAVE_LEVEL && room.disarmWave()) {
			events.publishEvent(new WaveTriggered(player));
			events.publishEvent(new ChaosEvent(ChaosEvent.WAVE, Map.of("by", player.name())));
			mutators.startRandom();
		}
		else if (level < WAVE_REARM_LEVEL) {
			room.rearmWave();
		}
		return view.in(lang);
	}

	private BoringThing find(long id) {
		return things.findById(id).orElseThrow(() -> new ThingNotFoundException(id));
	}

}
