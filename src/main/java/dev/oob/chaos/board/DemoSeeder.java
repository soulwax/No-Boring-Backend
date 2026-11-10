package dev.oob.chaos.board;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import dev.oob.chaos.i18n.Lang;
import dev.oob.chaos.player.Player;
import dev.oob.chaos.room.RoomState;
import dev.oob.chaos.twist.Genre;

/** Makes sure nobody ever opens the app to an empty (boring) board. Seeded in the default language. */
@Component
@ConditionalOnProperty(name = "chaos.seed-demo-data", havingValue = "true", matchIfMissing = true)
class DemoSeeder implements ApplicationRunner {

	private final BoringThingRepository things;

	private final ChaosService chaos;

	private final RoomState room;

	DemoSeeder(BoringThingRepository things, ChaosService chaos, RoomState room) {
		this.things = things;
		this.chaos = chaos;
		this.room = room;
	}

	/** Two instances booting against an empty database at once must not both seed it */
	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		room.lock();
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
