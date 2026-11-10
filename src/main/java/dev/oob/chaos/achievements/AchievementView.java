package dev.oob.chaos.achievements;

public record AchievementView(String code, String emoji, String title, String description, boolean unlocked) {

	static AchievementView of(Achievement achievement, boolean unlocked) {
		return new AchievementView(achievement.name(), achievement.emoji(), achievement.title(),
				achievement.description(), unlocked);
	}

}
