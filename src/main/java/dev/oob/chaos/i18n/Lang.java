package dev.oob.chaos.i18n;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** The languages the room speaks. German unless a client asks for English. */
public enum Lang {

	DE(Locale.GERMAN), EN(Locale.ENGLISH);

	public static final Lang DEFAULT = DE;

	public static final List<Locale> LOCALES = Arrays.stream(values()).map(Lang::locale).toList();

	private final Locale locale;

	Lang(Locale locale) {
		this.locale = locale;
	}

	public Locale locale() {
		return locale;
	}

	public static Lang of(Locale locale) {
		return locale != null && EN.locale.getLanguage().equals(locale.getLanguage()) ? EN : DEFAULT;
	}

	/** Lenient: {@code "en"}, {@code "EN"} or {@code "en-US"}; anything else (or nothing) is the default. */
	public static Lang parse(String tag) {
		return tag == null || tag.isBlank() ? DEFAULT : of(Locale.forLanguageTag(tag.strip()));
	}

}
