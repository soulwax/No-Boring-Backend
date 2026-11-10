package dev.oob.chaos.board;

import java.time.Instant;

/** What the outside world gets to see of a {@link BoringThing}. */
public record ThingView(Long id, String original, String twist, String author, int hype, int rerolls,
		Instant createdAt) {

	static ThingView of(BoringThing thing) {
		return new ThingView(thing.getId(), thing.getOriginal(), thing.getTwist(), thing.getAuthor(),
				thing.getHype(), thing.getRerolls(), thing.getCreatedAt());
	}

}
