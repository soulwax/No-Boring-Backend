package dev.oob.chaos.twist;

import static dev.oob.chaos.i18n.Text.of;

import java.util.List;

import dev.oob.chaos.i18n.Text;

/** A flavor of twist: an opener plus its own settings, constraints and stakes, in every language. */
public enum Genre {

	CLASSIC("🎪", of("Klassisch", "Classic"), of("", ""),
			List.of(of("auf einem Piratenschiff im Gewitter", "on a pirate ship in a thunderstorm"),
					of("in einer Schneekugel", "inside a snow globe"),
					of("um 3 Uhr nachts in einem verlassenen Möbelhaus", "at 3am in an abandoned furniture store"),
					of("auf der Mondoberfläche", "on the surface of the Moon"),
					of("im Finale des Synchronschwimmens", "during a synchronized-swimming final"),
					of("in einer viktorianischen Spukvilla", "in a haunted Victorian mansion"),
					of("auf dem Rücken eines sehr geduldigen Wals", "on the back of a very patient whale"),
					of("in einem verrauchten Jazzclub im Jahr 1959", "in a smoky jazz club in 1959"),
					of("in einem außer Kontrolle geratenen Heißluftballon", "on a runaway hot-air balloon"),
					of("in der Finalrunde einer Kochshow", "in the final round of a cooking show")),
			List.of(of("während ein Naturdoku-Sprecher alles kommentiert",
					"while narrated by a nature-documentary host"),
					of("aber jeder Satz muss sich reimen", "but every sentence must rhyme"),
					of("während eine Mariachi-Band auf jeden Schritt reagiert",
							"with a mariachi band reacting to each step"),
					of("ausschließlich per Ausdruckstanz", "using only interpretive dance"),
					of("unter Aufsicht eines Golden Retrievers", "while a golden retriever supervises"),
					of("in Zeitlupe, mit dramatischem Wind", "in slow motion, with dramatic wind"),
					of("gegen einen Countdown, der verdächtig nach Küchenwecker aussieht",
							"against a countdown that is suspiciously a kitchen timer"),
					of("dicht gefolgt von einer Blaskapelle", "with a marching band following close behind"),
					of("in voller Ritterrüstung", "while wearing a full suit of armor"),
					of("als live gestreamtes Sportevent mit Kommentar",
							"as a live-streamed sports event with commentary")),
			List.of(of("Gewinnst du, wirst du Bürgermeister.", "Win and you become mayor."),
					of("Verlierst du, trägst du eine Woche lang ein Cape.", "Lose and you wear a cape for a week."),
					of("Das Schicksal des Multiversums hängt davon ab.", "The fate of the multiverse depends on it."),
					of("Eine Horde Tauben wettet auf dich.", "A crowd of pigeons is betting on you."),
					of("Irgendwo hält ein Bösewicht einen Monolog darüber.",
							"Somewhere, a villain is monologuing about it."),
					of("Punkte gibt es nur für Stil.", "Points are awarded for style only."),
					of("Dein Erzrivale ist schon halb fertig.", "Your rival is already halfway done."),
					of("Dem Publikum wurde ein Feuerwerk versprochen.", "The audience has been promised fireworks."))),

	NOIR("🕵️", of("Noir", "Noir"), of("Es regnete schon wieder. ", "It was raining again. "),
			List.of(of("in einem verrauchten Büro über einem geschlossenen Waschsalon",
					"in a smoke-filled office above a closed laundromat"),
					of("unter einer flackernden Straßenlaterne an der 5th Avenue",
							"under a flickering streetlight on 5th Avenue"),
					of("in der hinteren Nische eines Nachtdiners", "in the back booth of an all-night diner"),
					of("auf einem nebelverhangenen Pier um Mitternacht", "on a fog-drowned pier at midnight"),
					of("in einer Hotellobby, die nach Ärger roch", "in a hotel lobby that smelled like trouble")),
			List.of(of("während irgendwo ein Saxofon jammert", "while a saxophone wails somewhere nearby"),
					of("den Hut tief ins Gesicht gezogen", "with a fedora pulled low over your eyes"),
					of("während du deine Gedanken laut erzählst", "while narrating your own thoughts out loud"),
					of("während ein Fremder mit einem Geheimnis von der Tür aus zusieht",
							"as a stranger with a secret watches from the doorway"),
					of("mit genau einer Tasse kaltem Kaffee Zeit",
							"with exactly one cup of cold coffee's worth of time")),
			List.of(of("Der Commissioner will bis zum Morgengrauen Antworten.",
					"The commissioner wants answers by dawn."),
					of("Alle lügen, sogar der Toaster.", "Everybody's lying, including the toaster."),
					of("Ein falscher Schritt, und die ganze Stadt weiß Bescheid.",
							"One wrong move and the whole city knows."),
					of("Irgendwo hält ein Jazzclub deinen Tisch frei.",
							"Somewhere a jazz club is holding your table."))),

	SPACE_OPERA("🚀", of("Weltraumoper", "Space Opera"), of("Sternzeit 4471. ", "Stardate 4471. "),
			List.of(of("auf der Brücke eines Raumschiffs, dem die Energie ausgeht",
					"on the bridge of a starship losing power"),
					of("in einer schwerelosen Cantina auf dem dritten Mond des Saturn",
							"in a zero-gravity cantina on Saturn's third moon"),
					of("in einem Wurmloch, das sich ständig wiederholt", "inside a wormhole that keeps looping"),
					of("an Bord eines Frachters voller verdächtiger Kisten",
							"aboard a freighter full of suspicious crates"),
					of("bei einer Anhörung im Galaktischen Senat", "at a galactic senate hearing")),
			List.of(of("während ein Android deine Technik kritisiert", "while an android critiques your technique"),
					of("während die Bord-KI darauf besteht zu singen", "as the ship's AI insists on singing"),
					of("mit Schilden bei 3 Prozent", "with the shields at 3 percent"),
					of("während ein winziger Alien alles falsch übersetzt",
							"while a tiny alien translates everything wrong"),
					of("in einem Raumanzug, der zwei Nummern zu klein ist", "in a spacesuit two sizes too small")),
			List.of(of("Das Schicksal von sieben Sternensystemen steht auf dem Spiel.",
					"The fate of seven star systems hangs in the balance."),
					of("Das Imperium schaut auf der Großleinwand zu.", "The Empire is watching on a big screen."),
					of("Scheiterst du, verlassen uns die Weltraumwale für immer.",
							"If you fail, the space whales leave forever."),
					of("Der rivalisierende Captain hat schon einen selbstgefälligen Bericht eingereicht.",
							"Your rival captain has already filed a smug report."))),

	NATURE_DOC("🦁", of("Naturdoku", "Nature Documentary"),
			of("Hier, in seinem natürlichen Lebensraum: ", "Here, in its natural habitat: "),
			List.of(of("in der windgepeitschten Serengeti", "on the windswept Serengeti"),
					of("tief im Blätterdach des Amazonas", "deep within the Amazon canopy"),
					of("an einem Wasserloch in der Abenddämmerung", "at a watering hole at dusk"),
					of("auf einem abgelegenen arktischen Schelfeis", "on a remote Arctic ice shelf"),
					of("in einem Korallenriff voller wertender Fische", "in a coral reef full of judgemental fish")),
			List.of(of("während ein flüsternder Erzähler jeden deiner Instinkte erklärt",
					"while a hushed narrator explains your every instinct"),
					of("während eine Gnuherde schweigend zusieht", "as a herd of wildebeest looks on in silence"),
					of("während dich ein neugieriger Gepard langsam beschleicht",
							"while slowly being stalked by a curious cheetah"),
					of("gefilmt in extremer Nahaufnahme und Zeitlupe", "filmed in extreme close-up and slow motion"),
					of("während sich ein Kamerateam in einem falschen Felsen versteckt",
							"while a camera crew hides in a fake rock")),
			List.of(of("Nur die Stärksten überstehen diese Saison.", "Only the strongest will survive the season."),
					of("Millionen Zuschauer halten gleichzeitig den Atem an.", "Millions of viewers gasp at once."),
					of("Die Natur findet, wie immer, einen Weg.", "Nature, as always, finds a way."),
					of("Diese Aufnahmen haben drei Jahre gedauert.", "This footage took three years to capture."))),

	MEDIEVAL("🏰", of("Mittelalter-Epos", "Medieval Epic"), of("Hört, hört! ", "Hear ye! "),
			List.of(of("im großen Saal einer bröckelnden Burg", "in the great hall of a crumbling castle"),
					of("auf einem von Drachen versengten Berg", "atop a dragon-scorched mountain"),
					of("bei einem königlichen Turnier vor dem ganzen Reich",
							"at a royal tournament before the whole kingdom"),
					of("in einer Taverne voller verdächtiger Barden", "in a tavern full of suspicious bards"),
					of("unter dem Blick eines uralten Zauberers", "beneath the gaze of an ancient wizard")),
			List.of(of("während ein Hofnarr jede deiner Bewegungen verspottet", "while a jester mocks your every move"),
					of("in einer kompletten, rostigen Rüstung", "wearing a full suit of rusty armor"),
					of("während ein Mönchschor unheilvoll singt", "as a choir of monks chants ominously"),
					of("während der Hofbarde eine Ballade darüber schreibt",
							"while the court bard writes a ballad about it"),
					of("mit einer Gans, die behauptet, adelig zu sein", "with a goose that claims to be royalty")),
			List.of(of("Der König hat das halbe Reich auf dich gesetzt.", "The king has wagered half the realm on you."),
					of("Scheiterst du, nennt man dich fortan Sir Faultier.",
							"Fail, and you shall be known as Sir Sloth."),
					of("Eine Prophezeiung hat genau diesen Tag vorhergesagt.", "A prophecy foretold this very day."),
					of("Der Drache hat zugesagt.", "The dragon has RSVP'd."))),

	HEIST("💎", of("Raubzug", "Heist"), of("Der Plan war simpel. ", "The plan was simple. "),
			List.of(of("im Tresor der langweiligsten Bank der Welt", "inside the vault of the world's most boring bank"),
					of("in einem Casino mit Laserschranken", "in a casino with laser-grid security"),
					of("auf einem Dach während eines stadtweiten Stromausfalls",
							"on a rooftop during a city-wide blackout"),
					of("nach Feierabend in einem Museum", "in a museum after hours"),
					of("in einem rasenden gepanzerten Zug", "on a speeding armored train")),
			List.of(of("während deine Crew über den Fluchtwagen streitet",
					"while your crew argues about the getaway car"),
					of("mit einem Countdown, der an die Wand gemalt ist", "with a countdown painted on the wall"),
					of("als Fensterputzer getarnt", "while pretending to be a window cleaner"),
					of("während der Wachmann ein sehr langes Gespräch anfängt",
							"as the security guard starts a very long conversation"),
					of("nur mit einer Büroklammer und Selbstvertrauen", "using only a paperclip and confidence")),
			List.of(of("Der einzige Ausweg führt mitten hindurch.", "There's no way out but through."),
					of("Dein Insider antwortet plötzlich nicht mehr auf Nachrichten.",
							"The inside man just stopped answering texts."),
					of("Klappt das, setzt du dich auf einer winzigen Insel zur Ruhe.",
							"Pull this off and you retire to a tiny island."),
					of("Der Detektiv ist dir nur einen Schritt hinterher.", "The detective is one step behind you.")));

	private final String emoji;

	private final Text label;

	private final Text opener;

	private final List<Text> settings;

	private final List<Text> constraints;

	private final List<Text> stakes;

	Genre(String emoji, Text label, Text opener, List<Text> settings, List<Text> constraints, List<Text> stakes) {
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

	public Text label() {
		return label;
	}

	public Text opener() {
		return opener;
	}

	public List<Text> settings() {
		return settings;
	}

	public List<Text> constraints() {
		return constraints;
	}

	public List<Text> stakes() {
		return stakes;
	}

}
