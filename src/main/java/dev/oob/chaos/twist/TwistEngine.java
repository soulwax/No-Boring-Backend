package dev.oob.chaos.twist;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Turns a boring activity into something nobody could possibly be bored by.
 * A twist is "{opener}{Activity} {setting}, {constraint}. {stakes}", with the word
 * lists taken from the chosen {@link Genre}.
 */
@Component
public class TwistEngine {

	private final Supplier<RandomGenerator> random;

	@Autowired
	public TwistEngine() {
		this(ThreadLocalRandom::current);
	}

	TwistEngine(Supplier<RandomGenerator> random) {
		this.random = random;
	}

	public String twist(String boringThing, Genre genre) {
		return "%s%s %s, %s. %s".formatted(genre.opener(), tidy(boringThing), pickFrom(genre.settings()),
				pickFrom(genre.constraints()), pickFrom(genre.stakes()));
	}

	/** The engine's dice, shared so every random choice in the app is reproducible in tests. */
	public <T> T pickFrom(List<T> options) {
		return options.get(random.get().nextInt(options.size()));
	}

	private static String tidy(String text) {
		String trimmed = text.strip().replaceAll("[.!?\\s]+$", "");
		if (trimmed.isEmpty()) {
			return trimmed;
		}
		return trimmed.substring(0, 1).toUpperCase(Locale.ROOT) + trimmed.substring(1);
	}

}
