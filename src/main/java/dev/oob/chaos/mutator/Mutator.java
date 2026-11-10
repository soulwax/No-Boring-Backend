package dev.oob.chaos.mutator;

/** A temporary rule change for the whole room. Only one is active at a time. */
public enum Mutator {

	DOUBLE_HYPE("⚡", "Double Hype", "Every hype counts twice."),
	FROZEN_TIME("🧊", "Frozen Time", "Hype stops fading. Lock in those gains."),
	GENRE_LOCK("🎬", "Genre Lock", "Every new twist is forced into one genre."),
	TWIST_TORNADO("🌀", "Twist Tornado", "Every hype re-rolls the twist. Hold on to something.");

	private final String emoji;

	private final String title;

	private final String description;

	Mutator(String emoji, String title, String description) {
		this.emoji = emoji;
		this.title = title;
		this.description = description;
	}

	public String emoji() {
		return emoji;
	}

	public String title() {
		return title;
	}

	public String description() {
		return description;
	}

}
