package dev.oob.chaos.live;

import org.jspecify.annotations.Nullable;

/**
 * How a committed {@link ChaosEvent} reaches the SSE clients. Called inside the transaction
 * (or straight away when there is none); implementations must not let clients see anything
 * before that transaction commits.
 */
interface RelayTransport {

	/**
	 * @param id the event's row in {@link ChaosLog}, or {@code null} for a derived event
	 * @param payload what was stored, or {@code null} for a derived event
	 */
	void deliver(String type, @Nullable Long id, @Nullable Object payload);

}
