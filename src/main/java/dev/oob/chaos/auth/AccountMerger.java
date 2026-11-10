package dev.oob.chaos.auth;

import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.oob.chaos.achievements.AchievementTracker;
import dev.oob.chaos.board.BoringThingRepository;
import dev.oob.chaos.player.Player;

/** Signing in keeps what you did as a guest: trophies, counters and your submissions. */
@Service
public class AccountMerger {

	/** Same shape the player header accepts; GitHub ids can't be passed off as guests */
	private static final Pattern GUEST_ID = Pattern.compile("(?!github-)[A-Za-z0-9-]{8,64}");

	private final AchievementTracker achievements;

	private final BoringThingRepository things;

	AccountMerger(AchievementTracker achievements, BoringThingRepository things) {
		this.achievements = achievements;
		this.things = things;
	}

	static boolean isGuestId(String id) {
		return GUEST_ID.matcher(id).matches();
	}

	@Transactional
	public void adopt(String guestId, Player account) {
		if (!isGuestId(guestId)) {
			return;
		}
		achievements.adopt(guestId, account);
		things.reassign(guestId, account.id());
	}

}
