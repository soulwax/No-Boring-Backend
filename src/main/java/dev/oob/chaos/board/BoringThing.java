package dev.oob.chaos.board;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import dev.oob.chaos.player.Player;
import dev.oob.chaos.twist.Genre;

@Entity
public class BoringThing {

	@Id
	@GeneratedValue
	private Long id;

	@Column(nullable = false, length = 140)
	private String original;

	@Column(nullable = false, length = 400)
	private String twist;

	@Column(nullable = false, length = 40)
	private String author;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Genre genre;

	private int hype;

	private int rerolls;

	/** Highest tier ever reached. Hype decays, legends don't. */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Tier peakTier = Tier.BORING;

	@Convert(converter = LinesConverter.class)
	@Column(nullable = false, length = 2000)
	private List<String> lore = List.of();

	@Column(nullable = false)
	private Instant createdAt;

	/** Submitting player's private id; never leaves the server. */
	@Column(length = 64)
	private String playerId;

	protected BoringThing() {
	}

	public BoringThing(String original, String twist, String author, Genre genre, String playerId) {
		this.original = original;
		this.twist = twist;
		this.author = author;
		this.genre = genre;
		this.playerId = playerId;
		this.createdAt = Instant.now();
	}

	public boolean isOwnedBy(Player player) {
		return player.isTracked() && player.id().equals(playerId);
	}

	public void addHype(int amount) {
		this.hype += amount;
	}

	public void retwist(String newTwist) {
		this.twist = newTwist;
		this.rerolls++;
	}

	/**
	 * Climbs to the tier the current hype deserves, writing one lore line for every tier
	 * passed on the way (a double-hype click can skip one).
	 *
	 * @return the newly reached peak tier, if this call raised it
	 */
	public Optional<Tier> evolve(Function<Tier, String> loreFor) {
		Tier deserved = Tier.forHype(hype);
		if (deserved.compareTo(peakTier) <= 0) {
			return Optional.empty();
		}
		List<String> grown = new ArrayList<>(lore);
		for (Tier tier : Tier.values()) {
			if (tier.compareTo(peakTier) > 0 && tier.compareTo(deserved) <= 0) {
				grown.add(loreFor.apply(tier));
			}
		}
		// A fresh list (not in-place mutation) so the converted column is reliably marked dirty
		this.lore = List.copyOf(grown);
		this.peakTier = deserved;
		return Optional.of(deserved);
	}

	public Long getId() {
		return id;
	}

	public String getOriginal() {
		return original;
	}

	public String getTwist() {
		return twist;
	}

	public String getAuthor() {
		return author;
	}

	public Genre getGenre() {
		return genre;
	}

	public int getHype() {
		return hype;
	}

	public int getRerolls() {
		return rerolls;
	}

	public Tier getPeakTier() {
		return peakTier;
	}

	public List<String> getLore() {
		return lore;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
