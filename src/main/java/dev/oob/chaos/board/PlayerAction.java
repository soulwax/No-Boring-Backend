package dev.oob.chaos.board;

import dev.oob.chaos.player.Player;

/** Domain event: a player did something on the board. */
public record PlayerAction(Player player, Kind kind, boolean ownThing) {

	public enum Kind {
		SUBMIT, HYPE, REROLL
	}

}
