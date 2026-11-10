package dev.oob.chaos.board;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.oob.chaos.live.ChaosEvent;
import dev.oob.chaos.twist.TwistEngine;

@Service
@Transactional
public class ChaosService {

	static final int HYPE_PER_CLICK = 3;

	static final int DECAY_PER_TICK = 1;

	private final BoringThingRepository things;

	private final TwistEngine twistEngine;

	private final ApplicationEventPublisher events;

	public ChaosService(BoringThingRepository things, TwistEngine twistEngine, ApplicationEventPublisher events) {
		this.things = things;
		this.twistEngine = twistEngine;
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

	public ThingView submit(String original, String author) {
		String name = (author == null || author.isBlank()) ? "Anonymous Legend" : author.strip();
		BoringThing thing = things.save(new BoringThing(original.strip(), twistEngine.twist(original), name));
		return changed(thing);
	}

	public ThingView hype(long id) {
		BoringThing thing = find(id);
		thing.addHype(HYPE_PER_CLICK);
		return changed(thing);
	}

	public ThingView reroll(long id) {
		BoringThing thing = find(id);
		thing.retwist(twistEngine.twist(thing.getOriginal()));
		return changed(thing);
	}

	/** Hype fades on its own, so the board is never the same twice. */
	@Scheduled(fixedRateString = "${chaos.decay-interval}", initialDelayString = "${chaos.decay-interval}")
	public void decay() {
		if (things.decayHype(DECAY_PER_TICK) > 0) {
			events.publishEvent(ChaosEvent.board(board()));
			events.publishEvent(ChaosEvent.meter(meter()));
		}
	}

	private ThingView changed(BoringThing thing) {
		things.flush();
		ThingView view = ThingView.of(thing);
		events.publishEvent(ChaosEvent.thing(view));
		events.publishEvent(ChaosEvent.meter(meter()));
		return view;
	}

	private BoringThing find(long id) {
		return things.findById(id).orElseThrow(() -> new ThingNotFoundException(id));
	}

}
