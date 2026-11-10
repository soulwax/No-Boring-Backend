package dev.oob.chaos.board;

import dev.oob.chaos.mutator.Mutator;
import dev.oob.chaos.player.Player;

/**
 * Domain event: a player did something on the board.
 *
 * @param thing the thing as it looks after the action
 * @param ownThing whether the player submitted that thing themselves
 * @param promotedTo the new peak tier if this action made the thing evolve, else {@code null}
 * @param mutator the mutator in effect at the time, else {@code null}
 */
public record PlayerAction(Player player, Kind kind, ThingView thing, boolean ownThing, Tier promotedTo,
		Mutator mutator) {

	public enum Kind {
		SUBMIT, HYPE, REROLL
	}

}
