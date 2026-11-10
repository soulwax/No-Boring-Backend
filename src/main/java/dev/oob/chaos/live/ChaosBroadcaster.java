package dev.oob.chaos.live;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** Fans {@link ChaosEvent}s out to every open Server-Sent Events connection. */
@Component
public class ChaosBroadcaster {

	private static final Logger log = LoggerFactory.getLogger(ChaosBroadcaster.class);

	private final Set<SseEmitter> emitters = ConcurrentHashMap.newKeySet();

	/**
	 * Opens a stream for one client and immediately primes it with the current
	 * state, so a fresh tab never has to wait for the next change.
	 */
	public SseEmitter subscribe(List<ChaosEvent> initialState) {
		SseEmitter emitter = new SseEmitter(0L);
		emitter.onCompletion(() -> drop(emitter));
		emitter.onTimeout(() -> drop(emitter));
		emitter.onError(ex -> drop(emitter));

		initialState.forEach(event -> send(emitter, event));
		emitters.add(emitter);
		broadcastPresence();
		return emitter;
	}

	@TransactionalEventListener(fallbackExecution = true)
	public void broadcast(ChaosEvent event) {
		emitters.forEach(emitter -> send(emitter, event));
	}

	public int subscriberCount() {
		return emitters.size();
	}

	/** Comments keep proxies from closing idle streams and flush out dead clients. */
	@Scheduled(fixedRate = 20_000)
	void heartbeat() {
		emitters.forEach(emitter -> {
			try {
				emitter.send(SseEmitter.event().comment("still not bored"));
			}
			catch (IOException | IllegalStateException ex) {
				drop(emitter);
			}
		});
	}

	private void send(SseEmitter emitter, ChaosEvent event) {
		try {
			emitter.send(SseEmitter.event().name(event.type()).data(event.payload(), MediaType.APPLICATION_JSON));
		}
		catch (IOException | IllegalStateException ex) {
			log.debug("Dropping SSE client: {}", ex.getMessage());
			drop(emitter);
		}
	}

	private void drop(SseEmitter emitter) {
		if (emitters.remove(emitter)) {
			broadcastPresence();
		}
	}

	private void broadcastPresence() {
		broadcast(new ChaosEvent("presence", Map.of("online", emitters.size())));
	}

}
