package dev.oob.chaos.live;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.function.Function;

import tools.jackson.databind.JsonNode;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import dev.oob.chaos.board.PlayerAction;
import dev.oob.chaos.board.WaveTriggered;
import dev.oob.chaos.i18n.Lang;
import dev.oob.chaos.i18n.Localized;
import dev.oob.chaos.i18n.Text;
import dev.oob.chaos.mutator.MutatorChanged;
import dev.oob.chaos.mutator.MutatorState;

/**
 * The room's story so far: turns domain events into short human-readable lines (in every
 * language) and broadcasts each one. The latest few for anyone who joins late come straight
 * from the {@link ChaosLog}, so every instance tells the same story.
 * Plain hypes are deliberately left out (far too chatty); evolutions are not.
 */
@Component
public class Chronicle {

	public record Entry(String emoji, String text, Instant at) {
	}

	static final int KEEP = 20;

	private static final int MAX_QUOTE = 40;

	private static final Text SUBMITTED = Text.of("%s hat %s entlangweilt", "%s un-bored %s");

	private static final Text REROLLED = Text.of("%s hat %s umgekrempelt", "%s plot-twisted %s");

	/** Arguments: thing, tier, player */
	private static final Text EVOLVED = Text.of("%1$s ist dank %3$s jetzt %2$s", "%1$s evolved to %2$s thanks to %3$s");

	private static final Text WAVE = Text.of("%s hat eine Chaoswelle ausgelöst", "%s unleashed a Chaos Wave");

	private static final Text MUTATOR_BEGAN = Text.of("%s hat begonnen", "%s has begun");

	private static final Text CALM = Text.of("Der Mutator ist verflogen. Die Ruhe kehrt zurück (vorerst)",
			"The mutator wore off. Calm returns (for now)");

	private static final Text QUOTE = Text.of("„%s“", "“%s”");

	private final ApplicationEventPublisher events;

	private final ChaosLog log;

	private final Clock clock;

	public Chronicle(ApplicationEventPublisher events, ChaosLog log, Clock clock) {
		this.events = events;
		this.log = log;
		this.clock = clock;
	}

	/** Newest first. */
	public Localized<List<JsonNode>> recent() {
		List<ChaosLog.Entry> latest = log.latest(ChaosEvent.ACTIVITY, KEEP);
		return lang -> latest.stream().map(entry -> entry.in(lang)).toList();
	}

	@EventListener
	void on(PlayerAction action) {
		String who = action.player().name();
		String original = action.thing().original();
		switch (action.kind()) {
			case SUBMIT -> write(action.thing().genre().emoji(),
					lang -> SUBMITTED.in(lang).formatted(who, quote(original, lang)));
			case REROLL -> write("🎲", lang -> REROLLED.in(lang).formatted(who, quote(original, lang)));
			case HYPE -> {
				if (action.promotedTo() != null) {
					write(action.promotedTo().emoji(), lang -> EVOLVED.in(lang)
						.formatted(quote(original, lang), action.promotedTo().label().in(lang), who));
				}
			}
		}
	}

	@EventListener
	void on(WaveTriggered wave) {
		write("🌊", lang -> WAVE.in(lang).formatted(wave.player().name()));
	}

	@EventListener
	void on(MutatorChanged change) {
		MutatorState state = change.state().in(Lang.DEFAULT);
		if (state.active()) {
			write(state.emoji(), lang -> MUTATOR_BEGAN.in(lang).formatted(change.state().in(lang).title()));
		}
		else {
			write("🕰️", CALM::in);
		}
	}

	private void write(String emoji, Function<Lang, String> text) {
		Instant at = clock.instant();
		events.publishEvent(new ChaosEvent(ChaosEvent.ACTIVITY,
				Localized.of(lang -> new Entry(emoji, text.apply(lang), at))));
	}

	private static String quote(String text, Lang lang) {
		String shortened = text.length() > MAX_QUOTE ? text.substring(0, MAX_QUOTE - 1) + "…" : text;
		return QUOTE.in(lang).formatted(shortened);
	}

}
