package dev.oob.chaos.achievements;

import java.util.function.Predicate;

public enum Achievement {

	BOREDOM_SLAYER("🗡️", "Boredom Slayer", "Submit your first boring thing", p -> p.submits >= 1),
	CONTENT_CREATOR("📼", "Content Creator", "Submit 5 boring things", p -> p.submits >= 5),
	HYPE_MACHINE("🔥", "Hype Machine", "Hand out 25 hypes", p -> p.hypes >= 25),
	PLOT_TWIST_ADDICT("🎲", "Plot Twist Addict", "Re-roll 5 twists", p -> p.rerolls >= 5),
	SHAMELESS("🪞", "Shameless", "Hype your own submission", p -> p.selfHypes >= 1),
	RENAISSANCE("🎭", "Renaissance Agent of Chaos", "Submit, hype and re-roll at least once",
			p -> p.submits >= 1 && p.hypes >= 1 && p.rerolls >= 1),
	WAVE_RIDER("🌊", "Wave Rider", "Be the one who pushes the Chaos Meter over the edge", p -> p.triggeredWave),
	MYTHMAKER("🐉", "Mythmaker", "Land the hype that makes something Mythic", p -> p.mythics >= 1),
	GENRE_HOPPER("🎬", "Genre Hopper", "Submit things in 4 different genres", p -> p.genres.size() >= 4),
	STORM_CHASER("🌀", "Storm Chaser", "Hype something while a mutator is active", p -> p.mutatorHypes >= 1),
	INSOMNIAC("🦉", "Insomniac", "Fight boredom between midnight and 5 a.m.", p -> p.nightOwl);

	private final String emoji;

	private final String title;

	private final String description;

	private final Predicate<PlayerProgress> earned;

	Achievement(String emoji, String title, String description, Predicate<PlayerProgress> earned) {
		this.emoji = emoji;
		this.title = title;
		this.description = description;
		this.earned = earned;
	}

	boolean isEarnedBy(PlayerProgress progress) {
		return earned.test(progress);
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
