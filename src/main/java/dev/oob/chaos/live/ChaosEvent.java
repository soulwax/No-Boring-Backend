package dev.oob.chaos.live;

import dev.oob.chaos.i18n.Lang;
import dev.oob.chaos.i18n.Localized;

/**
 * A change worth telling every connected client about. Published as a Spring
 * application event and pushed out over SSE once the transaction commits.
 *
 * @param type the SSE event name ({@code thing}, {@code board}, {@code meter}, {@code presence})
 * @param payload serialized to JSON as the event data; a {@link Localized} payload is sent to
 * each client in that client's language
 */
public record ChaosEvent(String type, Object payload) {

	/** The payload as a client reading {@code lang} receives it. */
	public Object payloadIn(Lang lang) {
		return payload instanceof Localized<?> localized ? localized.in(lang) : payload;
	}

	public static ChaosEvent thing(Object thing) {
		return new ChaosEvent("thing", thing);
	}

	public static ChaosEvent board(Object board) {
		return new ChaosEvent("board", board);
	}

	public static ChaosEvent meter(Object meter) {
		return new ChaosEvent("meter", meter);
	}

}
