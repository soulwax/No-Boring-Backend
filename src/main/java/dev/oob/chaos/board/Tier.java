package dev.oob.chaos.board;

import static dev.oob.chaos.i18n.Text.of;

import java.util.List;
import java.util.Optional;

import dev.oob.chaos.i18n.Text;

/**
 * Evolution stages a boring thing climbs as the crowd hypes it. Reaching a tier for the
 * first time writes one of its escalation lines into the thing's permanent lore.
 */
public enum Tier {

	BORING(0, "😐", of("Langweilig", "Boring"), List.of()),
	CURIOUS(6, "🤨", of("Neugierig", "Curious"), List.of(
			of("Eine kleine Menschenmenge hat sich versammelt.", "A small crowd has gathered."),
			of("Jemand hat einen Podcast darüber gestartet.", "Someone started a podcast about it."),
			of("Die Lokalzeitung schickt einen Praktikanten.", "A local newspaper sends an intern."))),
	SPICY(15, "🌶️", of("Scharf", "Spicy"), List.of(
			of("Ein Rivale taucht auf und macht genau dasselbe, nur schlechter.",
					"A rival appears, doing the exact same thing but worse."),
			of("Die Mariachi-Band ist der Gewerkschaft beigetreten.", "The mariachi band has unionized."),
			of("Es trendet in drei Ländern.", "It is trending in three countries."))),
	LEGENDARY(30, "👑", of("Legendär", "Legendary"), List.of(
			of("Ein Dokumentarfilm-Team folgt jetzt jedem deiner Schritte.",
					"A documentary crew now follows your every move."),
			of("Historiker streiten bereits darüber.", "Historians are already arguing about it."),
			of("Deine Oma ruft an, um zu sagen, dass sie stolz ist.", "Your grandmother calls to say she's proud."))),
	MYTHIC(50, "🐉", of("Mythisch", "Mythic"), List.of(
			of("Der Mond reicht eine Lärmbeschwerde ein.", "The Moon files a noise complaint."),
			of("Eine uralte Prophezeiung hat genau diesen Moment vorhergesagt.",
					"An ancient prophecy foretold this exact moment."),
			of("Die Zeit selbst hält inne, um zuzusehen.", "Time itself pauses to watch.")));

	private final int threshold;

	private final String emoji;

	private final Text label;

	private final List<Text> escalations;

	Tier(int threshold, String emoji, Text label, List<Text> escalations) {
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

	public Text label() {
		return label;
	}

	public List<Text> escalations() {
		return escalations;
	}

}
