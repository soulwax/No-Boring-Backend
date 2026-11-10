package dev.oob.chaos.achievements;

import java.time.Instant;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyEnumerated;

import org.jspecify.annotations.Nullable;

import dev.oob.chaos.twist.Genre;

/**
 * One player's counters and trophies. Rows are only changed while locked
 * ({@link PlayerProgressRepository#lock}), so two tabs of the same player can't lose each
 * other's progress.
 */
@Entity
public class PlayerProgress {

	@Id
	@Column(name = "player_id", length = 64)
	private String playerId;

	int submits;

	int hypes;

	int rerolls;

	int selfHypes;

	int mythics;

	int mutatorHypes;

	@Convert(converter = GenresConverter.class)
	@Column(nullable = false, length = 200)
	private Set<Genre> genres = EnumSet.noneOf(Genre.class);

	boolean triggeredWave;

	boolean nightOwl;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "player_achievement", joinColumns = @JoinColumn(name = "player_id"))
	@MapKeyColumn(name = "code", length = 40)
	@MapKeyEnumerated(EnumType.STRING)
	@Column(name = "unlocked_at", nullable = false)
	private Map<Achievement, Instant> unlocked = new HashMap<>();

	protected PlayerProgress() {
	}

	/** Applies a change and returns the achievements it newly unlocked, stamped {@code now}. */
	List<Achievement> record(Consumer<PlayerProgress> change, Instant now) {
		change.accept(this);
		List<Achievement> fresh = Arrays.stream(Achievement.values())
			.filter(a -> !unlocked.containsKey(a) && a.isEarnedBy(this))
			.toList();
		fresh.forEach(a -> unlocked.put(a, now));
		return fresh;
	}

	/**
	 * Folds a guest's progress into this account: counters add up, flags and genres combine, and
	 * each trophy keeps its earliest unlock.
	 */
	void absorb(PlayerProgress guest) {
		submits += guest.submits;
		hypes += guest.hypes;
		rerolls += guest.rerolls;
		selfHypes += guest.selfHypes;
		mythics += guest.mythics;
		mutatorHypes += guest.mutatorHypes;
		triggeredWave |= guest.triggeredWave;
		nightOwl |= guest.nightOwl;
		guest.genres.forEach(this::addGenre);
		guest.unlocked.forEach((a, at) -> unlocked.merge(a, at, (mine, theirs) -> mine.isBefore(theirs) ? mine : theirs));
	}

	void addGenre(Genre genre) {
		if (!genres.contains(genre)) {
			// A fresh set (not in-place mutation) so the converted column is reliably marked dirty
			Set<Genre> grown = EnumSet.of(genre);
			grown.addAll(genres);
			genres = grown;
		}
	}

	int genreCount() {
		return genres.size();
	}

	boolean has(Achievement achievement) {
		return unlocked.containsKey(achievement);
	}

	@Nullable Instant unlockedAt(Achievement achievement) {
		return unlocked.get(achievement);
	}

}
