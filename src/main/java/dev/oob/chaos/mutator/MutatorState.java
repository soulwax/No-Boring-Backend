package dev.oob.chaos.mutator;

import java.time.Instant;

import dev.oob.chaos.i18n.Lang;
import dev.oob.chaos.twist.Genre;

/** SSE payload for the "mutator" event, in one language; {@code active=false} means calm returned. */
public record MutatorState(boolean active, Mutator code, String emoji, String title, String description,
		Genre genre, Instant endsAt) {

	public static final MutatorState NONE = new MutatorState(false, null, null, null, null, null, null);

	static MutatorState of(Mutator mutator, Genre genre, Instant endsAt, Lang lang) {
		String title = genre == null ? mutator.title().in(lang)
				: mutator.title().in(lang) + ": " + genre.label().in(lang);
		return new MutatorState(true, mutator, mutator.emoji(), title, mutator.description().in(lang), genre, endsAt);
	}

}
