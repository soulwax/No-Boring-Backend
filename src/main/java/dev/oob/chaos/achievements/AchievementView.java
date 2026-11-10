package dev.oob.chaos.achievements;

import dev.oob.chaos.i18n.Lang;

public record AchievementView(String code, String emoji, String title, String description, boolean unlocked) {

	static AchievementView of(Achievement achievement, boolean unlocked, Lang lang) {
		return new AchievementView(achievement.name(), achievement.emoji(), achievement.title().in(lang),
				achievement.description().in(lang), unlocked);
	}

}
