package dev.oob.chaos.achievements;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import dev.oob.chaos.twist.Genre;

/** One player's counters. All access goes through the synchronized methods. */
final class PlayerProgress {

	int submits;

	int hypes;

	int rerolls;

	int selfHypes;

	int mythics;

	int mutatorHypes;

	final Set<Genre> genres = EnumSet.noneOf(Genre.class);

	boolean triggeredWave;

	boolean nightOwl;

	private final Set<Achievement> unlocked = EnumSet.noneOf(Achievement.class);

	/** Applies a change and returns the achievements it newly unlocked. */
	synchronized List<Achievement> record(Consumer<PlayerProgress> change) {
		change.accept(this);
		List<Achievement> fresh = Arrays.stream(Achievement.values())
			.filter(a -> !unlocked.contains(a) && a.isEarnedBy(this))
			.toList();
		unlocked.addAll(fresh);
		return fresh;
	}

	synchronized boolean has(Achievement achievement) {
		return unlocked.contains(achievement);
	}

}
