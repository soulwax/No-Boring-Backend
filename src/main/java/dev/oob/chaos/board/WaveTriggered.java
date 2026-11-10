package dev.oob.chaos.board;

import dev.oob.chaos.player.Player;

/** Domain event: this player's action pushed the Chaos Meter over the wave threshold. */
public record WaveTriggered(Player player) {
}
