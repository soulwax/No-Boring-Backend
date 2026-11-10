package dev.oob.chaos.mutator;

import org.jspecify.annotations.Nullable;
import java.time.Instant;

import dev.oob.chaos.i18n.Lang;
import dev.oob.chaos.twist.Genre;

/** SSE payload for the "mutator" event, in one language; {@code active=false} means calm returned. */
public record MutatorState(boolean active, @Nullable Mutator code, @Nullable String emoji, @Nullable String title,
		@Nullable String description, @Nullable Genre genre, @Nullable Instant endsAt) {

	public static final MutatorState NONE = new MutatorState(false, null, null, null, null, null, null);

	static MutatorState of(Mutator mutator, @Nullable Genre genre, Instant endsAt, Lang lang) {
		String title = genre == null ? mutator.title().in(lang)
				: mutator.title().in(lang) + ": " + genre.label().in(lang);
		return new MutatorState(true, mutator, mutator.emoji(), title, mutator.description().in(lang), genre, endsAt);
	}

}
