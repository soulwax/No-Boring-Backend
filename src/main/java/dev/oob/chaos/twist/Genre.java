package dev.oob.chaos.twist;

import java.util.List;

/** A flavor of twist: an opener plus its own settings, constraints and stakes. */
public enum Genre {

	CLASSIC("🎪", "Classic", "",
			List.of("on a pirate ship in a thunderstorm", "inside a snow globe",
					"at 3am in an abandoned furniture store", "on the surface of the Moon",
					"during a synchronized-swimming final", "in a haunted Victorian mansion",
					"on the back of a very patient whale", "in a smoky jazz club in 1959",
					"on a runaway hot-air balloon", "in the final round of a cooking show"),
			List.of("while narrated by a nature-documentary host", "but every sentence must rhyme",
					"with a mariachi band reacting to each step", "using only interpretive dance",
					"while a golden retriever supervises", "in slow motion, with dramatic wind",
					"against a countdown that is suspiciously a kitchen timer",
					"with a marching band following close behind", "while wearing a full suit of armor",
					"as a live-streamed sports event with commentary"),
			List.of("Win and you become mayor.", "Lose and you wear a cape for a week.",
					"The fate of the multiverse depends on it.", "A crowd of pigeons is betting on you.",
					"Somewhere, a villain is monologuing about it.", "Points are awarded for style only.",
					"Your rival is already halfway done.", "The audience has been promised fireworks.")),

	NOIR("🕵️", "Noir", "It was raining again. ",
			List.of("in a smoke-filled office above a closed laundromat",
					"under a flickering streetlight on 5th Avenue", "in the back booth of an all-night diner",
					"on a fog-drowned pier at midnight", "in a hotel lobby that smelled like trouble"),
			List.of("while a saxophone wails somewhere nearby", "with a fedora pulled low over your eyes",
					"while narrating your own thoughts out loud",
					"as a stranger with a secret watches from the doorway",
					"with exactly one cup of cold coffee's worth of time"),
			List.of("The commissioner wants answers by dawn.", "Everybody's lying, including the toaster.",
					"One wrong move and the whole city knows.", "Somewhere a jazz club is holding your table.")),

	SPACE_OPERA("🚀", "Space Opera", "Stardate 4471. ",
			List.of("on the bridge of a starship losing power", "in a zero-gravity cantina on Saturn's third moon",
					"inside a wormhole that keeps looping", "aboard a freighter full of suspicious crates",
					"at a galactic senate hearing"),
			List.of("while an android critiques your technique", "as the ship's AI insists on singing",
					"with the shields at 3 percent", "while a tiny alien translates everything wrong",
					"in a spacesuit two sizes too small"),
			List.of("The fate of seven star systems hangs in the balance.", "The Empire is watching on a big screen.",
					"If you fail, the space whales leave forever.",
					"Your rival captain has already filed a smug report.")),

	NATURE_DOC("🦁", "Nature Documentary", "Here, in its natural habitat: ",
			List.of("on the windswept Serengeti", "deep within the Amazon canopy", "at a watering hole at dusk",
					"on a remote Arctic ice shelf", "in a coral reef full of judgemental fish"),
			List.of("while a hushed narrator explains your every instinct",
					"as a herd of wildebeest looks on in silence", "while slowly being stalked by a curious cheetah",
					"filmed in extreme close-up and slow motion", "while a camera crew hides in a fake rock"),
			List.of("Only the strongest will survive the season.", "Millions of viewers gasp at once.",
					"Nature, as always, finds a way.", "This footage took three years to capture.")),

	MEDIEVAL("🏰", "Medieval Epic", "Hear ye! ",
			List.of("in the great hall of a crumbling castle", "atop a dragon-scorched mountain",
					"at a royal tournament before the whole kingdom", "in a tavern full of suspicious bards",
					"beneath the gaze of an ancient wizard"),
			List.of("while a jester mocks your every move", "wearing a full suit of rusty armor",
					"as a choir of monks chants ominously", "while the court bard writes a ballad about it",
					"with a goose that claims to be royalty"),
			List.of("The king has wagered half the realm on you.", "Fail, and you shall be known as Sir Sloth.",
					"A prophecy foretold this very day.", "The dragon has RSVP'd.")),

	HEIST("💎", "Heist", "The plan was simple. ",
			List.of("inside the vault of the world's most boring bank", "in a casino with laser-grid security",
					"on a rooftop during a city-wide blackout", "in a museum after hours",
					"on a speeding armored train"),
			List.of("while your crew argues about the getaway car", "with a countdown painted on the wall",
					"while pretending to be a window cleaner",
					"as the security guard starts a very long conversation",
					"using only a paperclip and confidence"),
			List.of("There's no way out but through.", "The inside man just stopped answering texts.",
					"Pull this off and you retire to a tiny island.", "The detective is one step behind you."));

	private final String emoji;

	private final String label;

	private final String opener;

	private final List<String> settings;

	private final List<String> constraints;

	private final List<String> stakes;

	Genre(String emoji, String label, String opener, List<String> settings, List<String> constraints,
			List<String> stakes) {
		this.emoji = emoji;
		this.label = label;
		this.opener = opener;
		this.settings = settings;
		this.constraints = constraints;
		this.stakes = stakes;
	}

	public String emoji() {
		return emoji;
	}

	public String label() {
		return label;
	}

	public String opener() {
		return opener;
	}

	public List<String> settings() {
		return settings;
	}

	public List<String> constraints() {
		return constraints;
	}

	public List<String> stakes() {
		return stakes;
	}

}
