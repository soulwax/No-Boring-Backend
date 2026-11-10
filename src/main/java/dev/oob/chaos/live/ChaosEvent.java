package dev.oob.chaos.live;

/**
 * A change worth telling every connected client about. Published as a Spring
 * application event and pushed out over SSE once the transaction commits.
 *
 * @param type the SSE event name ({@code thing}, {@code board}, {@code meter}, {@code presence})
 * @param payload serialized to JSON as the event data
 */
public record ChaosEvent(String type, Object payload) {

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
