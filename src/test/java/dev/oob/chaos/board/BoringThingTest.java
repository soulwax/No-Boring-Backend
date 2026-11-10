package dev.oob.chaos.board;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dev.oob.chaos.i18n.Lang;
import dev.oob.chaos.twist.Genre;
import dev.oob.chaos.twist.Twist;

class BoringThingTest {

	private final BoringThing thing = new BoringThing("ironing", new Twist(0, 1, 2), "Ada", Genre.SPACE_OPERA, null);

	@Test
	void evolvesOneTierAtATimeAndWritesLore() {
		thing.addHype(6);

		assertThat(thing.evolve(tier -> new Lore(tier, 1))).contains(Tier.CURIOUS);
		assertThat(thing.getLore()).containsExactly(new Lore(Tier.CURIOUS, 1));
		assertThat(thing.evolve(tier -> new Lore(tier, 0))).isEmpty();
	}

	@Test
	void aBigJumpWritesLoreForEveryTierSkipped() {
		thing.addHype(31);

		assertThat(thing.evolve(tier -> new Lore(tier, 0))).contains(Tier.LEGENDARY);
		assertThat(thing.getLore()).extracting(Lore::tier).containsExactly(Tier.CURIOUS, Tier.SPICY, Tier.LEGENDARY);
	}

	@Test
	void twistAndLoreReadInEveryLanguage() {
		thing.addHype(6);
		thing.evolve(tier -> new Lore(tier, 0));

		ThingView de = ThingView.of(thing, Lang.DE);
		ThingView en = ThingView.of(thing, Lang.EN);

		assertThat(de.twist()).isEqualTo("Sternzeit 4471. Ironing – auf der Brücke eines Raumschiffs, dem die Energie "
				+ "ausgeht, während die Bord-KI darauf besteht zu singen. Scheiterst du, verlassen uns die Weltraumwale "
				+ "für immer.");
		assertThat(en.twist()).isEqualTo("Stardate 4471. Ironing on the bridge of a starship losing power, as the "
				+ "ship's AI insists on singing. If you fail, the space whales leave forever.");
		assertThat(de.lore()).containsExactly("Eine kleine Menschenmenge hat sich versammelt.");
		assertThat(en.lore()).containsExactly("A small crowd has gathered.");
		assertThat(de.original()).isEqualTo(en.original()).isEqualTo("ironing");
	}

	@Test
	void peakTierSurvivesDecay() {
		thing.addHype(16);
		thing.evolve(tier -> new Lore(tier, 0));
		thing.addHype(-16);

		assertThat(Tier.forHype(thing.getHype())).isEqualTo(Tier.BORING);
		assertThat(thing.getPeakTier()).isEqualTo(Tier.SPICY);
		assertThat(thing.evolve(tier -> new Lore(tier, 0))).isEmpty();
	}

	@Test
	void mythicIsTheTop() {
		assertThat(Tier.forHype(10_000)).isEqualTo(Tier.MYTHIC);
		assertThat(Tier.MYTHIC.next()).isEmpty();
	}

}
