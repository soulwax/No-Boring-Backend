package dev.oob.chaos.board;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import dev.oob.chaos.player.Player;

/** Makes sure nobody ever opens the app to an empty (boring) board. */
@Component
@ConditionalOnProperty(name = "chaos.seed-demo-data", havingValue = "true", matchIfMissing = true)
class DemoSeeder implements ApplicationRunner {

	private final BoringThingRepository things;

	private final ChaosService chaos;

	DemoSeeder(BoringThingRepository things, ChaosService chaos) {
		this.things = things;
		this.chaos = chaos;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (things.count() > 0) {
			return;
		}
		chaos.submit("filing my taxes", "Accountant Ada", Player.ANONYMOUS);
		chaos.submit("waiting for the bus", "Commuter Kai", Player.ANONYMOUS);
		chaos.submit("folding laundry", "Sock Wizard", Player.ANONYMOUS);
		chaos.submit("a two-hour status meeting", "Product Pat", Player.ANONYMOUS);
		chaos.submit("defrosting the freezer", null, Player.ANONYMOUS);
	}

}
