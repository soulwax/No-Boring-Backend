package dev.oob.chaos.achievements;

import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

interface PlayerProgressRepository extends JpaRepository<PlayerProgress, String> {

	/** Two first-ever actions racing in from two tabs both succeed; one of them creates the row. */
	@Modifying
	@Query(value = "insert into player_progress (player_id) values (:id) on conflict do nothing", nativeQuery = true)
	void ensureExists(String id);

	/** {@code SELECT … FOR UPDATE}: held until the board's transaction commits */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from PlayerProgress p where p.playerId = :id")
	Optional<PlayerProgress> lock(String id);

}
