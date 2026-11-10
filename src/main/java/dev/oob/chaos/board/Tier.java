package dev.oob.chaos.board;

import java.util.List;
import java.util.Optional;

/**
 * Evolution stages a boring thing climbs as the crowd hypes it. Reaching a tier for the
 * first time writes one of its escalation lines into the thing's permanent lore.
 */
public enum Tier {

	BORING(0, "😐", "Boring", List.of()),
	CURIOUS(6, "🤨", "Curious", List.of(
			"A small crowd has gathered.",
			"Someone started a podcast about it.",
			"A local newspaper sends an intern.")),
	SPICY(15, "🌶️", "Spicy", List.of(
			"A rival appears, doing the exact same thing but worse.",
			"The mariachi band has unionized.",
			"It is trending in three countries.")),
	LEGENDARY(30, "👑", "Legendary", List.of(
			"A documentary crew now follows your every move.",
			"Historians are already arguing about it.",
			"Your grandmother calls to say she's proud.")),
	MYTHIC(50, "🐉", "Mythic", List.of(
			"The Moon files a noise complaint.",
			"An ancient prophecy foretold this exact moment.",
			"Time itself pauses to watch."));

	private final int threshold;

	private final String emoji;

	private final String label;

	private final List<String> escalations;

	Tier(int threshold, String emoji, String label, List<String> escalations) {
		this.threshold = threshold;
		this.emoji = emoji;
		this.label = label;
		this.escalations = escalations;
	}

	public static Tier forHype(int hype) {
		Tier reached = BORING;
		for (Tier tier : values()) {
			if (hype >= tier.threshold) {
				reached = tier;
			}
		}
		return reached;
	}

	public Optional<Tier> next() {
		return ordinal() + 1 < values().length ? Optional.of(values()[ordinal() + 1]) : Optional.empty();
	}

	public int threshold() {
		return threshold;
	}

	public String emoji() {
		return emoji;
	}

	public String label() {
		return label;
	}

	public List<String> escalations() {
		return escalations;
	}

}
