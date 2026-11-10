package dev.oob.chaos.board;

import java.time.Instant;
import java.util.List;

import dev.oob.chaos.twist.Genre;

/**
 * What the outside world gets to see of a {@link BoringThing}.
 *
 * @param tier the tier current hype qualifies for (drops as hype decays)
 * @param peakTier the highest tier ever reached (never drops)
 * @param nextTierAt hype needed for the next tier, or {@code null} at the top
 */
public record ThingView(Long id, String original, String twist, String author, Genre genre, int hype,
		int rerolls, Tier tier, Tier peakTier, Integer nextTierAt, List<String> lore, Instant createdAt) {

	static ThingView of(BoringThing thing) {
		Tier tier = Tier.forHype(thing.getHype());
		Integer nextTierAt = tier.next().map(Tier::threshold).orElse(null);
		return new ThingView(thing.getId(), thing.getOriginal(), thing.getTwist(), thing.getAuthor(),
				thing.getGenre(), thing.getHype(), thing.getRerolls(), tier, thing.getPeakTier(), nextTierAt,
				thing.getLore(), thing.getCreatedAt());
	}

}
