package dev.oob.chaos.twist;

import java.util.Locale;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import dev.oob.chaos.i18n.Lang;
import dev.oob.chaos.i18n.Text;

/**
 * A rolled twist, stored as positions in its {@link Genre}'s word lists rather than as text,
 * so the same twist can be read in every language. Reads as "{opener}{Activity} {setting},
 * {constraint}. {stakes}" (German sets the setting off with a dash).
 */
@Embeddable
public record Twist(
		@Column(name = "twist_setting") int setting,
		@Column(name = "twist_constraint") int constraint,
		@Column(name = "twist_stakes") int stakes) {

	private static final Text FORMAT = Text.of("%s%s – %s, %s. %s", "%s%s %s, %s. %s");

	public String in(Lang lang, Genre genre, String boringThing) {
		return FORMAT.in(lang).formatted(genre.opener().in(lang), tidy(boringThing),
				genre.settings().get(setting).in(lang), genre.constraints().get(constraint).in(lang),
				genre.stakes().get(stakes).in(lang));
	}

	private static String tidy(String text) {
		String trimmed = text.strip().replaceAll("[.!?\\s]+$", "");
		if (trimmed.isEmpty()) {
			return trimmed;
		}
		return trimmed.substring(0, 1).toUpperCase(Locale.ROOT) + trimmed.substring(1);
	}

}
