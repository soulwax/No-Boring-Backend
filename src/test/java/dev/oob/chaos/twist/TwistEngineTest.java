package dev.oob.chaos.twist;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Random;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class TwistEngineTest {

	@Test
	void twistKeepsTheOriginalAndAddsSettingConstraintAndStakes() {
		Random seeded = new Random(42);
		TwistEngine engine = new TwistEngine(() -> seeded);

		String twist = engine.twist("  filing my taxes.  ", Genre.CLASSIC);

		assertThat(twist).startsWith("Filing my taxes ");
		assertThat(Genre.CLASSIC.settings()).anyMatch(twist::contains);
		assertThat(Genre.CLASSIC.constraints()).anyMatch(twist::contains);
		assertThat(Genre.CLASSIC.stakes()).anyMatch(twist::endsWith);
	}

	@ParameterizedTest
	@EnumSource(Genre.class)
	void everyGenreUsesItsOwnOpenerAndVocabulary(Genre genre) {
		String twist = new TwistEngine().twist("waiting in line", genre);

		assertThat(twist).startsWith(genre.opener() + "Waiting in line ");
		assertThat(genre.settings()).anyMatch(twist::contains);
		assertThat(genre.stakes()).anyMatch(twist::endsWith);
	}

	@Test
	void sameSeedSameTwist() {
		Random a = new Random(7);
		Random b = new Random(7);

		assertThat(new TwistEngine(() -> a).twist("waiting", Genre.NOIR))
			.isEqualTo(new TwistEngine(() -> b).twist("waiting", Genre.NOIR));
	}

}
