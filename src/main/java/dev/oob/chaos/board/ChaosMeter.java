package dev.oob.chaos.board;

import dev.oob.chaos.i18n.Lang;
import dev.oob.chaos.i18n.Text;

/**
 * Global room energy. {@code level} is 0-100 and approaches 100 asymptotically as
 * total hype grows, so the meter never quite "maxes out".
 */
public record ChaosMeter(int level, String mood, long totalHype, long things) {

	private static final double HALF_ENERGY = 35.0;

	public static ChaosMeter from(long totalHype, long things, Lang lang) {
		int level = (int) Math.round(100 * (1 - Math.pow(0.5, totalHype / HALF_ENERGY)));
		return new ChaosMeter(level, moodFor(level).in(lang), totalHype, things);
	}

	private static Text moodFor(int level) {
		if (level < 20) {
			return Text.of("Gähnende Leere", "Snoozefest");
		}
		if (level < 45) {
			return Text.of("Leicht pikant", "Mildly Spicy");
		}
		if (level < 70) {
			return Text.of("Chaotisch gut", "Chaotic Good");
		}
		if (level < 90) {
			return Text.of("Totales Chaos", "Absolute Mayhem");
		}
		return Text.of("Hitzetod der Langeweile", "Heat Death of Boredom");
	}

}
