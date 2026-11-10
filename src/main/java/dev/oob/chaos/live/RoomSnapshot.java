package dev.oob.chaos.live;

import java.util.Map;

import org.springframework.stereotype.Component;

import dev.oob.chaos.board.ChaosService;
import dev.oob.chaos.i18n.Localized;
import dev.oob.chaos.mutator.MutatorDirector;

/** Renders a {@link ChaosEvent#DERIVED} event's payload from the current state of the room. */
@Component
class RoomSnapshot {

	private final ChaosService chaos;

	private final MutatorDirector mutators;

	private final Presence presence;

	RoomSnapshot(ChaosService chaos, MutatorDirector mutators, Presence presence) {
		this.chaos = chaos;
		this.mutators = mutators;
		this.presence = presence;
	}

	Localized<?> of(String type) {
		return switch (type) {
			case ChaosEvent.BOARD -> chaos.board();
			case ChaosEvent.METER -> chaos.meter();
			case ChaosEvent.MUTATOR -> mutators.state();
			case ChaosEvent.PRESENCE -> {
				Map<String, Integer> online = Map.of("online", presence.total());
				yield lang -> online;
			}
			default -> throw new IllegalArgumentException("Not a derived event: " + type);
		};
	}

}
