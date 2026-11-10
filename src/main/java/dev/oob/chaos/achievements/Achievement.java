package dev.oob.chaos.achievements;

import static dev.oob.chaos.i18n.Text.of;

import java.util.function.Predicate;

import dev.oob.chaos.i18n.Text;

public enum Achievement {

	BOREDOM_SLAYER("🗡️", of("Langeweile-Bezwinger", "Boredom Slayer"),
			of("Reiche dein erstes langweiliges Ding ein", "Submit your first boring thing"), p -> p.submits >= 1),
	CONTENT_CREATOR("📼", of("Content Creator", "Content Creator"),
			of("Reiche 5 langweilige Dinge ein", "Submit 5 boring things"), p -> p.submits >= 5),
	HYPE_MACHINE("🔥", of("Hype-Maschine", "Hype Machine"), of("Verteile 25 Hypes", "Hand out 25 hypes"),
			p -> p.hypes >= 25),
	PLOT_TWIST_ADDICT("🎲", of("Plot-Twist-Junkie", "Plot Twist Addict"), of("Würfle 5 Twists neu", "Re-roll 5 twists"),
			p -> p.rerolls >= 5),
	SHAMELESS("🪞", of("Schamlos", "Shameless"), of("Hype deine eigene Einreichung", "Hype your own submission"),
			p -> p.selfHypes >= 1),
	RENAISSANCE("🎭", of("Universalgenie des Chaos", "Renaissance Agent of Chaos"),
			of("Reiche ein, hype und würfle je mindestens einmal", "Submit, hype and re-roll at least once"),
			p -> p.submits >= 1 && p.hypes >= 1 && p.rerolls >= 1),
	WAVE_RIDER("🌊", of("Wellenreiter", "Wave Rider"),
			of("Bring das Chaos-Meter selbst zum Überlaufen", "Be the one who pushes the Chaos Meter over the edge"),
			p -> p.triggeredWave),
	MYTHMAKER("🐉", of("Mythenschmied", "Mythmaker"),
			of("Lande den Hype, der etwas mythisch macht", "Land the hype that makes something Mythic"),
			p -> p.mythics >= 1),
	GENRE_HOPPER("🎬", of("Genre-Hopper", "Genre Hopper"),
			of("Reiche Dinge in 4 verschiedenen Genres ein", "Submit things in 4 different genres"),
			p -> p.genres.size() >= 4),
	STORM_CHASER("🌀", of("Sturmjäger", "Storm Chaser"),
			of("Hype etwas, während ein Mutator aktiv ist", "Hype something while a mutator is active"),
			p -> p.mutatorHypes >= 1),
	INSOMNIAC("🦉", of("Schlaflos", "Insomniac"),
			of("Bekämpfe die Langeweile zwischen Mitternacht und 5 Uhr früh", "Fight boredom between midnight and 5 a.m."),
			p -> p.nightOwl);

	private final String emoji;

	private final Text title;

	private final Text description;

	private final Predicate<PlayerProgress> earned;

	Achievement(String emoji, Text title, Text description, Predicate<PlayerProgress> earned) {
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

	public Text title() {
		return title;
	}

	public Text description() {
		return description;
	}

}
