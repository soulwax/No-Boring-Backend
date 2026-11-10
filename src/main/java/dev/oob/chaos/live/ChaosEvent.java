package dev.oob.chaos.live;

import java.util.Set;

import org.jspecify.annotations.Nullable;

import dev.oob.chaos.i18n.Lang;
import dev.oob.chaos.i18n.Localized;

/**
 * A change worth telling every connected client about. Published as a Spring application
 * event; {@link ChaosRelay} hands it to the transport inside the transaction, and clients
 * receive it once that transaction commits.
 *
 * @param type the SSE event name
 * @param payload serialized to JSON as the event data; a {@link Localized} payload is sent to
 * each client in that client's language. {@code null} for {@link #DERIVED} types, whose data
 * every instance reads fresh from the database instead.
 */
public record ChaosEvent(String type, @Nullable Object payload) {

	public static final String BOARD = "board";

	public static final String METER = "meter";

	public static final String MUTATOR = "mutator";

	public static final String PRESENCE = "presence";

	public static final String CHRONICLE = "chronicle";

	public static final String THING = "thing";

	public static final String ACTIVITY = "activity";

	public static final String ACHIEVEMENT = "achievement";

	public static final String WAVE = "wave";

	/** State, not news: only "it changed" travels, each instance renders its own snapshot. */
	public static final Set<String> DERIVED = Set.of(BOARD, METER, MUTATOR, PRESENCE);

	public static ChaosEvent changed(String type) {
		if (!DERIVED.contains(type)) {
			throw new IllegalArgumentException(type + " carries a payload");
		}
		return new ChaosEvent(type, null);
	}

	public static ChaosEvent thing(Object thing) {
		return new ChaosEvent(THING, thing);
	}

	public boolean derived() {
		return payload == null;
	}

	/** The payload as a client reading {@code lang} receives it. */
	public @Nullable Object payloadIn(Lang lang) {
		return payload instanceof Localized<?> localized ? localized.in(lang) : payload;
	}

}
