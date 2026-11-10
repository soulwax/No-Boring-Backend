package dev.oob.chaos.player;

import org.jspecify.annotations.Nullable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import dev.oob.chaos.i18n.Lang;
import dev.oob.chaos.i18n.Text;

/**
 * Whoever is clicking. The browser invents a random {@code id} and keeps it; there are no
 * accounts. Untracked players (no id) can still play but earn no achievements.
 */
public record Player(@Nullable String id, String name) {

	private static final Text DEFAULT_NAME = Text.of("Anonyme Legende", "Anonymous Legend");

	public static final Player ANONYMOUS = new Player(null, defaultName(Lang.DEFAULT));

	/** What nameless players are called, in the language they play in. */
	public static String defaultName(Lang lang) {
		return DEFAULT_NAME.in(lang);
	}

	public boolean isTracked() {
		return id != null;
	}

	/**
	 * Public, non-reversible handle for this player. Broadcast instead of the id so other
	 * clients can't impersonate anyone; the owner's browser computes the same hash to
	 * recognize its own events.
	 */
	public @Nullable String tag() {
		if (id == null) {
			return null;
		}
		try {
			byte[] hash = MessageDigest.getInstance("SHA-256").digest(id.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hash, 0, 6);
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException(ex);
		}
	}

}
