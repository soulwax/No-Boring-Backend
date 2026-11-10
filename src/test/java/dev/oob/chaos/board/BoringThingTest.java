package dev.oob.chaos.board;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dev.oob.chaos.twist.Genre;

class BoringThingTest {

	private final BoringThing thing = new BoringThing("ironing", "Ironing in space", "Ada", Genre.SPACE_OPERA, null);

	@Test
	void evolvesOneTierAtATimeAndWritesLore() {
		thing.addHype(6);

		assertThat(thing.evolve(tier -> tier.label() + " lore")).contains(Tier.CURIOUS);
		assertThat(thing.getLore()).containsExactly("Curious lore");
		assertThat(thing.evolve(tier -> "again")).isEmpty();
	}

	@Test
	void aBigJumpWritesLoreForEveryTierSkipped() {
		thing.addHype(31);

		assertThat(thing.evolve(Tier::label)).contains(Tier.LEGENDARY);
		assertThat(thing.getLore()).containsExactly("Curious", "Spicy", "Legendary");
	}

	@Test
	void peakTierSurvivesDecay() {
		thing.addHype(16);
		thing.evolve(Tier::label);
		thing.addHype(-16);

		assertThat(Tier.forHype(thing.getHype())).isEqualTo(Tier.BORING);
		assertThat(thing.getPeakTier()).isEqualTo(Tier.SPICY);
		assertThat(thing.evolve(Tier::label)).isEmpty();
	}

	@Test
	void mythicIsTheTop() {
		assertThat(Tier.forHype(10_000)).isEqualTo(Tier.MYTHIC);
		assertThat(Tier.MYTHIC.next()).isEmpty();
	}

}
