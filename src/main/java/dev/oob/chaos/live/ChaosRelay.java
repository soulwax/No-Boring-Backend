package dev.oob.chaos.live;

import java.util.Objects;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Where every {@link ChaosEvent} leaves the domain: just before the publishing transaction
 * commits, events with a payload are written to the {@link ChaosLog} outbox (inside that same
 * transaction), then handed to the transport. Events published outside a transaction go out
 * immediately.
 */
@Component
class ChaosRelay {

	private final ChaosLog log;

	private final RelayTransport transport;

	private final MeterRegistry meters;

	ChaosRelay(ChaosLog log, RelayTransport transport, MeterRegistry meters) {
		this.log = log;
		this.transport = transport;
		this.meters = meters;
	}

	@TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
	void relay(ChaosEvent event) {
		Counter.builder("chaos.events").description("Events relayed to the room").tag("type", event.type())
			.register(meters)
			.increment();
		if (event.derived()) {
			transport.deliver(event.type(), null, null);
			return;
		}
		Object payload = Objects.requireNonNull(event.payload());
		transport.deliver(event.type(), log.append(event.type(), payload), payload);
	}

}
