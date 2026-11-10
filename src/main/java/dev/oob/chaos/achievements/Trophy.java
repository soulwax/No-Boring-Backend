package dev.oob.chaos.achievements;

import java.time.Instant;

import org.jspecify.annotations.Nullable;

import dev.oob.chaos.i18n.Lang;

/** API version 2 of a trophy: like {@link AchievementView}, plus when it was earned. */
public record Trophy(String code, String emoji, String title, String description, @Nullable Instant unlockedAt) {

	static Trophy of(Achievement achievement, @Nullable Instant unlockedAt, Lang lang) {
		return new Trophy(achievement.name(), achievement.emoji(), achievement.title().in(lang),
				achievement.description().in(lang), unlockedAt);
	}

}
