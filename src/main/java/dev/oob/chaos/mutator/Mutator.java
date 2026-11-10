package dev.oob.chaos.mutator;

import static dev.oob.chaos.i18n.Text.of;

import dev.oob.chaos.i18n.Text;

/** A temporary rule change for the whole room. Only one is active at a time. */
public enum Mutator {

	DOUBLE_HYPE("⚡", of("Doppel-Hype", "Double Hype"), of("Jeder Hype zählt doppelt.", "Every hype counts twice.")),
	FROZEN_TIME("🧊", of("Eingefrorene Zeit", "Frozen Time"),
			of("Der Hype verblasst nicht mehr. Sichert eure Gewinne.", "Hype stops fading. Lock in those gains.")),
	GENRE_LOCK("🎬", of("Genre-Sperre", "Genre Lock"),
			of("Jeder neue Twist wird in ein einziges Genre gezwungen.", "Every new twist is forced into one genre.")),
	TWIST_TORNADO("🌀", of("Twist-Tornado", "Twist Tornado"),
			of("Jeder Hype würfelt den Twist neu. Gut festhalten.",
					"Every hype re-rolls the twist. Hold on to something."));

	private final String emoji;

	private final Text title;

	private final Text description;

	Mutator(String emoji, Text title, Text description) {
		this.emoji = emoji;
		this.title = title;
		this.description = description;
	}

	public String emoji() {
		return emoji;
	}

	public Text title() {
		return title;
	}

	public Text description() {
		return description;
	}

}
