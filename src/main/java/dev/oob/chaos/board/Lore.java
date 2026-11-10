package dev.oob.chaos.board;

import dev.oob.chaos.i18n.Lang;

/**
 * One line of a thing's lore: which of its tier's escalations was written. Stored as a key
 * like {@code SPICY:2}, so the line can be read in every language.
 */
public record Lore(Tier tier, int line) {

	public static Lore parse(String key) {
		int colon = key.indexOf(':');
		return new Lore(Tier.valueOf(key.substring(0, colon)), Integer.parseInt(key.substring(colon + 1)));
	}

	public String key() {
		return tier.name() + ":" + line;
	}

	public String in(Lang lang) {
		return tier.escalations().get(line).in(lang);
	}

}
