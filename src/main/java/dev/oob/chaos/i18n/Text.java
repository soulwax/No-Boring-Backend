package dev.oob.chaos.i18n;

/** One piece of copy in every {@link Lang}. */
public record Text(String de, String en) {

	public static Text of(String de, String en) {
		return new Text(de, en);
	}

	public String in(Lang lang) {
		return lang == Lang.EN ? en : de;
	}

}
