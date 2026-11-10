package dev.oob.chaos.player;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Whoever is clicking. The browser invents a random {@code id} and keeps it; there are no
 * accounts. Untracked players (no id) can still play but earn no achievements.
 */
public record Player(String id, String name) {

	public static final String DEFAULT_NAME = "Anonymous Legend";

	public static final Player ANONYMOUS = new Player(null, DEFAULT_NAME);

	public boolean isTracked() {
		return id != null;
	}

	/**
	 * Public, non-reversible handle for this player. Broadcast instead of the id so other
	 * clients can't impersonate anyone; the owner's browser computes the same hash to
	 * recognize its own events.
	 */
	public String tag() {
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
