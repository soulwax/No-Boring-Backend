package dev.oob.chaos.twist;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Random;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import dev.oob.chaos.i18n.Lang;
import dev.oob.chaos.i18n.Text;

class TwistEngineTest {

	@Test
	void twistKeepsTheOriginalAndAddsSettingConstraintAndStakes() {
		Random seeded = new Random(42);
		Twist twist = new TwistEngine(() -> seeded).roll(Genre.CLASSIC);

		String en = twist.in(Lang.EN, Genre.CLASSIC, "  filing my taxes.  ");

		assertThat(en).startsWith("Filing my taxes ");
		assertThat(Genre.CLASSIC.settings()).extracting(Text::en).anyMatch(en::contains);
		assertThat(Genre.CLASSIC.constraints()).extracting(Text::en).anyMatch(en::contains);
		assertThat(Genre.CLASSIC.stakes()).extracting(Text::en).anyMatch(en::endsWith);
	}

	@ParameterizedTest
	@EnumSource(Genre.class)
	void everyGenreUsesItsOwnOpenerAndVocabularyInEveryLanguage(Genre genre) {
		Twist twist = new TwistEngine().roll(genre);

		assertThat(twist.in(Lang.EN, genre, "waiting in line")).startsWith(genre.opener().en() + "Waiting in line ")
			.endsWith(genre.stakes().get(twist.stakes()).en());
		assertThat(twist.in(Lang.DE, genre, "Schlange stehen")).startsWith(genre.opener().de() + "Schlange stehen – ")
			.contains(genre.settings().get(twist.setting()).de())
			.endsWith(genre.stakes().get(twist.stakes()).de());
	}

	@ParameterizedTest
	@EnumSource(Genre.class)
	void everyPhraseIsTranslated(Genre genre) {
		assertThat(genre.settings()).isNotEmpty().allMatch(TwistEngineTest::translated);
		assertThat(genre.constraints()).isNotEmpty().allMatch(TwistEngineTest::translated);
		assertThat(genre.stakes()).isNotEmpty().allMatch(TwistEngineTest::translated);
	}

	@Test
	void sameSeedSameTwist() {
		Random a = new Random(7);
		Random b = new Random(7);

		assertThat(new TwistEngine(() -> a).roll(Genre.NOIR)).isEqualTo(new TwistEngine(() -> b).roll(Genre.NOIR));
	}

	private static boolean translated(Text text) {
		return !text.de().isBlank() && !text.en().isBlank() && !text.de().equals(text.en());
	}

}
