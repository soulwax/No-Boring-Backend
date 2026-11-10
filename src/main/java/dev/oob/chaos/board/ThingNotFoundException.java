package dev.oob.chaos.board;

public class ThingNotFoundException extends RuntimeException {

	private final long id;

	public ThingNotFoundException(long id) {
		super("Boring thing #" + id + " does not exist.");
		this.id = id;
	}

	public long id() {
		return id;
	}

}
