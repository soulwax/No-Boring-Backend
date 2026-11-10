package dev.oob.chaos.twist;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Turns a boring activity into something nobody could possibly be bored by, by rolling a
 * {@link Twist} from the chosen {@link Genre}'s word lists.
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

	public Twist roll(Genre genre) {
		return new Twist(pickIndex(genre.settings()), pickIndex(genre.constraints()), pickIndex(genre.stakes()));
	}

	/** The engine's dice, shared so every random choice in the app is reproducible in tests. */
	public <T> T pickFrom(List<T> options) {
		return options.get(pickIndex(options));
	}

	public int pickIndex(List<?> options) {
		return random.get().nextInt(options.size());
	}

}
