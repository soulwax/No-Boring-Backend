package dev.oob.chaos.twist;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Random;

import org.junit.jupiter.api.Test;

class TwistEngineTest {

	@Test
	void twistKeepsTheOriginalAndAddsSettingConstraintAndStakes() {
		Random seeded = new Random(42);
		TwistEngine engine = new TwistEngine(() -> seeded);

		String twist = engine.twist("  filing my taxes.  ");

		assertThat(twist).startsWith("Filing my taxes ");
		assertThat(TwistEngine.SETTINGS).anyMatch(twist::contains);
		assertThat(TwistEngine.CONSTRAINTS).anyMatch(twist::contains);
		assertThat(TwistEngine.STAKES).anyMatch(twist::endsWith);
	}

	@Test
	void sameSeedSameTwist() {
		Random a = new Random(7);
		Random b = new Random(7);

		assertThat(new TwistEngine(() -> a).twist("waiting"))
			.isEqualTo(new TwistEngine(() -> b).twist("waiting"));
	}

}
