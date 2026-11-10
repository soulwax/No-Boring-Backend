package dev.oob.chaos.achievements;

import dev.oob.chaos.player.Player;

/** Broadcast to everyone; {@code playerTag} lets the unlocker's own tab recognize it. */
public record AchievementUnlocked(String playerTag, String playerName, String code, String emoji, String title,
		String description) {

	static AchievementUnlocked of(Player player, Achievement achievement) {
		return new AchievementUnlocked(player.tag(), player.name(), achievement.name(), achievement.emoji(),
				achievement.title(), achievement.description());
	}

}
