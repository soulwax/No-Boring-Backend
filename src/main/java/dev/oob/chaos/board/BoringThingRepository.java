package dev.oob.chaos.board;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface BoringThingRepository extends JpaRepository<BoringThing, Long> {

	List<BoringThing> findTop50ByOrderByHypeDescCreatedAtDesc();

	@Query("select coalesce(sum(t.hype), 0) from BoringThing t")
	long totalHype();

	/** Entropy: every hype counter drifts back toward zero. */
	@Modifying(clearAutomatically = true)
	@Query("""
			update BoringThing t
			set t.hype = case when t.hype > :amount then t.hype - :amount else 0 end
			where t.hype > 0""")
	int decayHype(int amount);

}
