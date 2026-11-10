package dev.oob.chaos.board;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.oob.chaos.i18n.Lang;
import dev.oob.chaos.i18n.Localized;
import dev.oob.chaos.live.ChaosEvent;
import dev.oob.chaos.mutator.Mutator;
import dev.oob.chaos.mutator.MutatorDirector;
import dev.oob.chaos.player.Player;
import dev.oob.chaos.twist.Genre;
import dev.oob.chaos.twist.TwistEngine;

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

	private final AtomicBoolean waveArmed = new AtomicBoolean(true);

	public ChaosService(BoringThingRepository things, TwistEngine twistEngine, MutatorDirector mutators,
			ApplicationEventPublisher events) {
		this.things = things;
		this.twistEngine = twistEngine;
		this.mutators = mutators;
		this.events = events;
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
	public ThingView submit(String original, String author, Genre requested, Player player, Lang lang) {
		String name = (author == null || author.isBlank()) ? player.name() : author.strip();
		Genre genre = mutators.lockedGenre()
			.orElseGet(() -> requested != null ? requested : twistEngine.pickFrom(GENRES));
		BoringThing thing = things.save(
				new BoringThing(original.strip(), twistEngine.roll(genre), name, genre, player.id()));
		return changed(thing, player, lang, PlayerAction.Kind.SUBMIT, null);
	}

	public ThingView hype(long id, Player player, Lang lang) {
		BoringThing thing = find(id);
		thing.addHype(HYPE_PER_CLICK * mutators.hypeMultiplier());
		if (mutators.isActive(Mutator.TWIST_TORNADO)) {
			thing.retwist(twistEngine.roll(genreFor(thing)));
		}
		Tier promoted = thing.evolve(this::loreFor).orElse(null);
		return changed(thing, player, lang, PlayerAction.Kind.HYPE, promoted);
	}

	public ThingView reroll(long id, Player player, Lang lang) {
		BoringThing thing = find(id);
		thing.retwist(twistEngine.roll(genreFor(thing)));
		return changed(thing, player, lang, PlayerAction.Kind.REROLL, null);
	}

	/** Hype fades on its own, so the board is never the same twice. Unless time is frozen. */
	@Scheduled(fixedRateString = "${chaos.decay-interval}", initialDelayString = "${chaos.decay-interval}")
	public void decay() {
		if (mutators.isActive(Mutator.FROZEN_TIME)) {
			return;
		}
		if (things.decayHype(DECAY_PER_TICK) > 0) {
			Localized<ChaosMeter> meter = meter();
			events.publishEvent(ChaosEvent.board(board()));
			events.publishEvent(ChaosEvent.meter(meter));
			if (meter.in(Lang.DEFAULT).level() < WAVE_REARM_LEVEL) {
				waveArmed.set(true);
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
	private ThingView changed(BoringThing thing, Player player, Lang lang, PlayerAction.Kind kind, Tier promoted) {
		things.flush();
		Localized<ThingView> view = Localized.of(l -> ThingView.of(thing, l));
		Localized<ChaosMeter> meter = meter();
		int level = meter.in(Lang.DEFAULT).level();
		events.publishEvent(ChaosEvent.thing(view));
		events.publishEvent(ChaosEvent.meter(meter));
		events.publishEvent(new PlayerAction(player, kind, view.in(lang), thing.isOwnedBy(player), promoted,
				mutators.activeMutator().orElse(null)));

		if (level >= WAVE_LEVEL && waveArmed.compareAndSet(true, false)) {
			events.publishEvent(new WaveTriggered(player));
			events.publishEvent(new ChaosEvent("wave", Map.of("by", player.name())));
			mutators.startRandom();
		}
		else if (level < WAVE_REARM_LEVEL) {
			waveArmed.set(true);
		}
		return view.in(lang);
	}

	private BoringThing find(long id) {
		return things.findById(id).orElseThrow(() -> new ThingNotFoundException(id));
	}

}
