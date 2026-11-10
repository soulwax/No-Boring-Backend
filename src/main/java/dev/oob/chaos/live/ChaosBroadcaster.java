package dev.oob.chaos.live;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import dev.oob.chaos.i18n.Lang;
import dev.oob.chaos.i18n.Localized;

/**
 * This instance's open Server-Sent Events connections, each in its own language. What reaches
 * them is decided elsewhere ({@link RelayTransport}); this class only fans out and keeps the
 * room's presence count honest.
 */
@Component
public class ChaosBroadcaster {

	private static final Logger log = LoggerFactory.getLogger(ChaosBroadcaster.class);

	/** Missed while reconnecting, worth catching up on: the rest is in the fresh snapshot */
	private static final List<String> REPLAYED = List.of(ChaosEvent.ACHIEVEMENT);

	private static final int MAX_REPLAY = 50;

	private final Map<SseEmitter, Lang> emitters = new ConcurrentHashMap<>();

	private final RoomSnapshot snapshot;

	private final Chronicle chronicle;

	private final ChaosLog chaosLog;

	private final Presence presence;

	private final ApplicationEventPublisher events;

	/** The room-wide total last announced, so heartbeats only speak up when it changed */
	private final AtomicInteger announcedOnline = new AtomicInteger(-1);

	ChaosBroadcaster(RoomSnapshot snapshot, Chronicle chronicle, ChaosLog chaosLog, Presence presence,
			ApplicationEventPublisher events, MeterRegistry meters) {
		this.snapshot = snapshot;
		this.chronicle = chronicle;
		this.chaosLog = chaosLog;
		this.presence = presence;
		this.events = events;
		Gauge.builder("chaos.sse.clients", emitters, Map::size)
			.description("SSE connections held by this instance")
			.register(meters);
	}

	/**
	 * Opens a stream for one client and immediately primes it with the current state, so a fresh
	 * tab never has to wait for the next change. A browser reconnecting on its own sends
	 * {@code Last-Event-ID}; it then also gets the achievements announced while it was away.
	 */
	public SseEmitter subscribe(Lang lang, @Nullable Long lastEventId) {
		SseEmitter emitter = new SseEmitter(0L);
		emitter.onCompletion(() -> drop(emitter));
		emitter.onTimeout(() -> drop(emitter));
		emitter.onError(ex -> drop(emitter));

		for (String type : List.of(ChaosEvent.BOARD, ChaosEvent.METER, ChaosEvent.MUTATOR)) {
			send(emitter, lang, type, null, snapshot.of(type));
		}
		send(emitter, lang, ChaosEvent.CHRONICLE, null, chronicle.recent());
		if (lastEventId != null) {
			chaosLog.since(lastEventId, REPLAYED, MAX_REPLAY).forEach(e -> send(emitter, lang, e.type(), e.id(), e));
		}
		emitters.put(emitter, lang);
		reportPresence(true);
		return emitter;
	}

	/** Sends one event to every client of this instance, each in its own language. */
	public void broadcast(String type, @Nullable Long id, Localized<?> payload) {
		emitters.forEach((emitter, lang) -> send(emitter, lang, type, id, payload));
	}

	public int subscriberCount() {
		return emitters.size();
	}

	/** Comments keep proxies from closing idle streams and flush out dead clients. */
	@Scheduled(fixedRate = 20_000)
	void heartbeat() {
		emitters.keySet().forEach(emitter -> {
			try {
				emitter.send(SseEmitter.event().comment("still not bored"));
			}
			catch (IOException | IllegalStateException ex) {
				drop(emitter);
			}
		});
		// Also keeps this instance's row fresh, and notices instances that vanished
		reportPresence(false);
	}

	private void send(SseEmitter emitter, Lang lang, String type, @Nullable Long id, Localized<?> payload) {
		try {
			SseEmitter.SseEventBuilder event = SseEmitter.event().name(type);
			if (id != null) {
				event.id(String.valueOf(id));
			}
			emitter.send(event.data(payload.in(lang), MediaType.APPLICATION_JSON));
		}
		catch (IOException | IllegalStateException ex) {
			log.debug("Dropping SSE client: {}", ex.getMessage());
			drop(emitter);
		}
	}

	private void drop(SseEmitter emitter) {
		if (emitters.remove(emitter) != null) {
			reportPresence(true);
		}
	}

	private void reportPresence(boolean changed) {
		try {
			presence.report(emitters.size());
			int total = presence.total();
			if (announcedOnline.getAndSet(total) != total || changed) {
				events.publishEvent(ChaosEvent.changed(ChaosEvent.PRESENCE));
			}
		}
		catch (RuntimeException ex) {
			// Presence is a nicety; a database blip must never break a stream
			log.debug("Could not update presence: {}", ex.getMessage());
		}
	}

}
