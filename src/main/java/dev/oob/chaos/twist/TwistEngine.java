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
 * A twist is "{activity} {setting}, {constraint}. {stakes}".
 */
@Component
public class TwistEngine {

	static final List<String> SETTINGS = List.of(
			"on a pirate ship in a thunderstorm",
			"inside a snow globe",
			"at 3am in an abandoned furniture store",
			"on the surface of the Moon",
			"during a synchronized-swimming final",
			"in a haunted Victorian mansion",
			"on the back of a very patient whale",
			"in a smoky jazz club in 1959",
			"on a runaway hot-air balloon",
			"in the final round of a cooking show");

	static final List<String> CONSTRAINTS = List.of(
			"while narrated by a nature-documentary host",
			"but every sentence must rhyme",
			"with a mariachi band reacting to each step",
			"using only interpretive dance",
			"while a golden retriever supervises",
			"in slow motion, with dramatic wind",
			"against a countdown that is suspiciously a kitchen timer",
			"with a marching band following close behind",
			"while wearing a full suit of armor",
			"as a live-streamed sports event with commentary");

	static final List<String> STAKES = List.of(
			"Win and you become mayor.",
			"Lose and you wear a cape for a week.",
			"The fate of the multiverse depends on it.",
			"A crowd of pigeons is betting on you.",
			"Somewhere, a villain is monologuing about it.",
			"Points are awarded for style only.",
			"Your rival is already halfway done.",
			"The audience has been promised fireworks.");

	private final Supplier<RandomGenerator> random;

	@Autowired
	public TwistEngine() {
		this(ThreadLocalRandom::current);
	}

	TwistEngine(Supplier<RandomGenerator> random) {
		this.random = random;
	}

	public String twist(String boringThing) {
		return "%s %s, %s. %s".formatted(
				tidy(boringThing), pick(SETTINGS), pick(CONSTRAINTS), pick(STAKES));
	}

	private String pick(List<String> options) {
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
