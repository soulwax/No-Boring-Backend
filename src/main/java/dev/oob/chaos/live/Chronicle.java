package dev.oob.chaos.live;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import dev.oob.chaos.board.PlayerAction;
import dev.oob.chaos.board.WaveTriggered;
import dev.oob.chaos.mutator.MutatorChanged;

/**
 * The room's story so far: turns domain events into short human-readable lines,
 * broadcasts each one, and remembers the latest few for anyone who joins late.
 * Plain hypes are deliberately left out (far too chatty); evolutions are not.
 */
@Component
public class Chronicle {

	public record Entry(String emoji, String text, Instant at) {
	}

	static final int KEEP = 20;

	private static final int MAX_QUOTE = 40;

	private final Deque<Entry> recent = new ArrayDeque<>();

	private final ApplicationEventPublisher events;

	private final Clock clock;

	public Chronicle(ApplicationEventPublisher events, Clock clock) {
		this.events = events;
		this.clock = clock;
	}

	/** Newest first. */
	public synchronized List<Entry> recent() {
		return List.copyOf(recent);
	}

	@EventListener
	void on(PlayerAction action) {
		String who = action.player().name();
		String what = quote(action.thing().original());
		switch (action.kind()) {
			case SUBMIT -> write(action.thing().genre().emoji(), "%s un-bored %s".formatted(who, what));
			case REROLL -> write("🎲", "%s plot-twisted %s".formatted(who, what));
			case HYPE -> {
				if (action.promotedTo() != null) {
					write(action.promotedTo().emoji(),
							"%s evolved to %s thanks to %s".formatted(what, action.promotedTo().label(), who));
				}
			}
		}
	}

	@EventListener
	void on(WaveTriggered wave) {
		write("🌊", "%s unleashed a Chaos Wave".formatted(wave.player().name()));
	}

	@EventListener
	void on(MutatorChanged change) {
		if (change.state().active()) {
			write(change.state().emoji(), "%s has begun".formatted(change.state().title()));
		}
		else {
			write("🕰️", "The mutator wore off. Calm returns (for now)");
		}
	}

	private void write(String emoji, String text) {
		Entry entry = new Entry(emoji, text, clock.instant());
		synchronized (this) {
			recent.addFirst(entry);
			while (recent.size() > KEEP) {
				recent.removeLast();
			}
		}
		events.publishEvent(new ChaosEvent("activity", entry));
	}

	private static String quote(String text) {
		String shortened = text.length() > MAX_QUOTE ? text.substring(0, MAX_QUOTE - 1) + "…" : text;
		return "“" + shortened + "”";
	}

}
