package dev.oob.chaos.live;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import dev.oob.chaos.i18n.Localized;

/**
 * Single-instance delivery: renders the payload now, inside the transaction (so a snapshot
 * sees exactly what is about to be committed and needs no second connection), and hands it to
 * this instance's clients after the commit.
 */
@Component
@ConditionalOnProperty(name = "chaos.relay.mode", havingValue = "local", matchIfMissing = true)
class LocalTransport implements RelayTransport {

	private final ChaosBroadcaster broadcaster;

	private final RoomSnapshot snapshot;

	LocalTransport(ChaosBroadcaster broadcaster, RoomSnapshot snapshot) {
		this.broadcaster = broadcaster;
		this.snapshot = snapshot;
	}

	@Override
	public void deliver(String type, @Nullable Long id, @Nullable Object payload) {
		Localized<?> rendered = payload == null ? snapshot.of(type)
				: payload instanceof Localized<?> l ? l : lang -> payload;
		Runnable send = () -> broadcaster.broadcast(type, id, rendered);
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					send.run();
				}
			});
		}
		else {
			send.run();
		}
	}

}
