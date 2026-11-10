package dev.oob.chaos.board;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import dev.oob.chaos.i18n.Lang;
import dev.oob.chaos.player.Player;
import dev.oob.chaos.twist.Genre;

/** Makes sure nobody ever opens the app to an empty (boring) board. Seeded in the default language. */
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
		chaos.submit("die Steuererklärung machen", "Buchhalterin Ada", Genre.NOIR, Player.ANONYMOUS, Lang.DEFAULT);
		chaos.submit("auf den Bus warten", "Pendler Kai", Genre.NATURE_DOC, Player.ANONYMOUS, Lang.DEFAULT);
		chaos.submit("Wäsche zusammenlegen", "Sockenzauberer", Genre.MEDIEVAL, Player.ANONYMOUS, Lang.DEFAULT);
		chaos.submit("ein zweistündiges Status-Meeting", "Product Pat", Genre.HEIST, Player.ANONYMOUS, Lang.DEFAULT);
		chaos.submit("den Gefrierschrank abtauen", null, Genre.SPACE_OPERA, Player.ANONYMOUS, Lang.DEFAULT);
	}

}
