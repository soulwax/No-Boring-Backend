package dev.oob.chaos.i18n;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

/**
 * A value that reads differently per {@link Lang}. Used as a {@code ChaosEvent} payload, it
 * lets the broadcaster hand every SSE client the version in that client's language.
 */
@FunctionalInterface
public interface Localized<T> {

	T in(Lang lang);

	/**
	 * Renders every language right now and keeps the results, so the value is a snapshot:
	 * safe to send after the transaction (and the entities it read) are gone.
	 */
	static <T> Localized<T> of(Function<Lang, T> render) {
		Map<Lang, T> rendered = new EnumMap<>(Lang.class);
		for (Lang lang : Lang.values()) {
			rendered.put(lang, render.apply(lang));
		}
		return rendered::get;
	}

}
