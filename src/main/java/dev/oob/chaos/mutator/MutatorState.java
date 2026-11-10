package dev.oob.chaos.mutator;

import java.time.Instant;

import dev.oob.chaos.twist.Genre;

/** SSE payload for the "mutator" event; {@code active=false} means calm returned. */
public record MutatorState(boolean active, Mutator code, String emoji, String title, String description,
		Genre genre, Instant endsAt) {

	public static final MutatorState NONE = new MutatorState(false, null, null, null, null, null, null);

	static MutatorState of(Mutator mutator, Genre genre, Instant endsAt) {
		String title = genre == null ? mutator.title() : mutator.title() + ": " + genre.label();
		return new MutatorState(true, mutator, mutator.emoji(), title, mutator.description(), genre, endsAt);
	}

}
