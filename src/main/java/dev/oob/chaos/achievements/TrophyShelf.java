package dev.oob.chaos.achievements;

import java.util.List;

/** {@code GET /api/achievements} with {@code API-Version: 2}: the whole shelf, counted for you. */
public record TrophyShelf(int unlocked, int total, List<Trophy> trophies) {
}
