package dev.oob.chaos.board;

/**
 * Global room energy. {@code level} is 0-100 and approaches 100 asymptotically as
 * total hype grows, so the meter never quite "maxes out".
 */
public record ChaosMeter(int level, String mood, long totalHype, long things) {

	private static final double HALF_ENERGY = 35.0;

	public static ChaosMeter from(long totalHype, long things) {
		int level = (int) Math.round(100 * (1 - Math.pow(0.5, totalHype / HALF_ENERGY)));
		return new ChaosMeter(level, moodFor(level), totalHype, things);
	}

	private static String moodFor(int level) {
		if (level < 20) {
			return "Snoozefest";
		}
		if (level < 45) {
			return "Mildly Spicy";
		}
		if (level < 70) {
			return "Chaotic Good";
		}
		if (level < 90) {
			return "Absolute Mayhem";
		}
		return "Heat Death of Boredom";
	}

}
