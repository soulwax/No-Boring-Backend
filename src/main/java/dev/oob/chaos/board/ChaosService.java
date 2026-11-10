package dev.oob.chaos.board;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
	public List<ThingView> board() {
		return things.findTop50ByOrderByHypeDescCreatedAtDesc().stream().map(ThingView::of).toList();
	}

	@Transactional(readOnly = true)
	public ChaosMeter meter() {
		return ChaosMeter.from(things.totalHype(), things.count());
	}

	/** @param requested the genre the player picked, or {@code null} for a surprise */
	public ThingView submit(String original, String author, Genre requested, Player player) {
		String name = (author == null || author.isBlank()) ? player.name() : author.strip();
		Genre genre = mutators.lockedGenre()
			.orElseGet(() -> requested != null ? requested : twistEngine.pickFrom(GENRES));
		BoringThing thing = things.save(
				new BoringThing(original.strip(), twistEngine.twist(original, genre), name, genre, player.id()));
		return changed(thing, player, PlayerAction.Kind.SUBMIT, null);
	}

	public ThingView hype(long id, Player player) {
		BoringThing thing = find(id);
		thing.addHype(HYPE_PER_CLICK * mutators.hypeMultiplier());
		if (mutators.isActive(Mutator.TWIST_TORNADO)) {
			thing.retwist(twistEngine.twist(thing.getOriginal(), genreFor(thing)));
		}
		Tier promoted = thing.evolve(tier -> twistEngine.pickFrom(tier.escalations())).orElse(null);
		return changed(thing, player, PlayerAction.Kind.HYPE, promoted);
	}

	public ThingView reroll(long id, Player player) {
		BoringThing thing = find(id);
		thing.retwist(twistEngine.twist(thing.getOriginal(), genreFor(thing)));
		return changed(thing, player, PlayerAction.Kind.REROLL, null);
	}

	/** Hype fades on its own, so the board is never the same twice. Unless time is frozen. */
	@Scheduled(fixedRateString = "${chaos.decay-interval}", initialDelayString = "${chaos.decay-interval}")
	public void decay() {
		if (mutators.isActive(Mutator.FROZEN_TIME)) {
			return;
		}
		if (things.decayHype(DECAY_PER_TICK) > 0) {
			ChaosMeter meter = meter();
			events.publishEvent(ChaosEvent.board(board()));
			events.publishEvent(ChaosEvent.meter(meter));
			if (meter.level() < WAVE_REARM_LEVEL) {
				waveArmed.set(true);
			}
		}
	}

	/** Re-rolls stay in the thing's own genre, unless a genre lock says otherwise. */
	private Genre genreFor(BoringThing thing) {
		return mutators.lockedGenre().orElse(thing.getGenre());
	}

	private ThingView changed(BoringThing thing, Player player, PlayerAction.Kind kind, Tier promoted) {
		things.flush();
		ThingView view = ThingView.of(thing);
		ChaosMeter meter = meter();
		events.publishEvent(ChaosEvent.thing(view));
		events.publishEvent(ChaosEvent.meter(meter));
		events.publishEvent(new PlayerAction(player, kind, view, thing.isOwnedBy(player), promoted,
				mutators.activeMutator().orElse(null)));

		if (meter.level() >= WAVE_LEVEL && waveArmed.compareAndSet(true, false)) {
			events.publishEvent(new WaveTriggered(player));
			events.publishEvent(new ChaosEvent("wave", Map.of("by", player.name())));
			mutators.startRandom();
		}
		else if (meter.level() < WAVE_REARM_LEVEL) {
			waveArmed.set(true);
		}
		return view;
	}

	private BoringThing find(long id) {
		return things.findById(id).orElseThrow(() -> new ThingNotFoundException(id));
	}

}
