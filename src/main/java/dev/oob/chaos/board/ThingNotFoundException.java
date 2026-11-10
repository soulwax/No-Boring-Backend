package dev.oob.chaos.board;

public class ThingNotFoundException extends RuntimeException {

	public ThingNotFoundException(long id) {
		super("Boring thing #" + id + " does not exist (or it got too exciting and left).");
	}

}
