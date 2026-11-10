package dev.oob.chaos.board;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import dev.oob.chaos.player.Player;

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

	private int hype;

	private int rerolls;

	@Column(nullable = false)
	private Instant createdAt;

	/** Submitting player's private id; never leaves the server. */
	@Column(length = 64)
	private String playerId;

	protected BoringThing() {
	}

	public BoringThing(String original, String twist, String author, String playerId) {
		this.original = original;
		this.twist = twist;
		this.author = author;
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

	public int getHype() {
		return hype;
	}

	public int getRerolls() {
		return rerolls;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
