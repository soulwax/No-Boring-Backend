package dev.oob.chaos.live;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * {@code /actuator/health/relay}: whether this instance currently hears the rest of the room.
 * A reconnecting listener reports UNKNOWN rather than DOWN: the instance still serves its own
 * clients, so it must not be pulled from rotation for it.
 */
@Component("relay")
@ConditionalOnProperty(name = "chaos.relay.mode", havingValue = "postgres")
class RelayHealthIndicator implements HealthIndicator {

	private final PostgresListener listener;

	RelayHealthIndicator(PostgresListener listener) {
		this.listener = listener;
	}

	@Override
	public Health health() {
		return listener.isListening() ? Health.up().withDetail("transport", "LISTEN/NOTIFY").build()
				: Health.unknown().withDetail("transport", "reconnecting").build();
	}

}
