package dev.oob.chaos.quest;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.oob.chaos.achievements.AchievementTracker;
import dev.oob.chaos.board.BoringThing;
import dev.oob.chaos.board.BoringThingRepository;
import dev.oob.chaos.board.ThingView;
import dev.oob.chaos.edge.EdgeAiClient;
import dev.oob.chaos.i18n.Lang;
import dev.oob.chaos.i18n.Localized;
import dev.oob.chaos.i18n.Text;
import dev.oob.chaos.live.ChaosEvent;
import dev.oob.chaos.live.Chronicle;
import dev.oob.chaos.player.Player;

@Service
@Transactional
public class QuestService {

	private final BoringThingRepository things;
	private final EdgeAiClient edgeAi;
	private final AchievementTracker achievements;
	private final ApplicationEventPublisher events;

	public QuestService(
			BoringThingRepository things,
			EdgeAiClient edgeAi,
			AchievementTracker achievements,
			ApplicationEventPublisher events) {
		this.things = things;
		this.edgeAi = edgeAi;
		this.achievements = achievements;
		this.events = events;
	}

	public record QuestOverview(
			List<ThingView> myActiveQuests,
			List<ThingView> mySlayedQuests,
			List<ThingView> recentHallOfFame,
			int slayedCount,
			int slayerXp,
			int slayerLevel) {}

	/**
	 * Slays a real-world chore: awards Slayer XP, unlocks achievements, and broadcasts to room.
	 */
	public ThingView slay(long id, Player player, Lang lang) {
		BoringThing thing = things.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Chore not found: " + id));

		boolean newlySlayed = thing.slay(player.name());
		if (newlySlayed && player.isTracked()) {
			achievements.recordSlay(player.id(), 100);
		}

		ThingView view = ThingView.of(thing, lang);

		// Broadcast update to all connected players via SSE outbox
		events.publishEvent(ChaosEvent.thing(Localized.of(l -> ThingView.of(thing, l))));
		Instant now = Instant.now();
		events.publishEvent(ChaosEvent.activity(Localized.of(l -> new Chronicle.Entry(
				"⚔️",
				Text.of("%s hat die Aufgabe bezwungen: %s!", "%s slayed the chore: %s!").in(l)
						.formatted(player.name(), thing.getOriginal()),
				now
		))));
		events.publishEvent(ChaosEvent.changed(ChaosEvent.BOARD));

		return view;
	}

	/**
	 * Generates Flux Schnell artwork stored in Cloudflare R2 for this quest.
	 */
	public ThingView visualize(long id, Player player, Lang lang) {
		BoringThing thing = things.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Chore not found: " + id));

		String prompt = "Cinematic epic illustration of " + thing.getOriginal() + " in " + thing.getGenre().name() + " visual style";
		EdgeAiClient.ArtworkResult art = edgeAi.generateArtwork(thing.getId(), prompt);
		if (art.success() && art.url() != null) {
			thing.setImageUrl(art.url());
			events.publishEvent(ChaosEvent.thing(Localized.of(l -> ThingView.of(thing, l))));
			events.publishEvent(ChaosEvent.changed(ChaosEvent.BOARD));
		}

		return ThingView.of(thing, lang);
	}

	/**
	 * Exports the quest as an RFC 5545 iCalendar (.ics) invite.
	 */
	@Transactional(readOnly = true)
	public String exportIcs(long id, Lang lang) {
		BoringThing thing = things.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Chore not found: " + id));

		String twistText = thing.getTwist().in(lang, thing.getGenre(), thing.getOriginal());
		String steps = String.join("\\n- ", thing.getQuestSteps());
		String now = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
				.withZone(ZoneOffset.UTC)
				.format(Instant.now());

		return """
				BEGIN:VCALENDAR
				VERSION:2.0
				PRODID:-//Opposite of Boring//Chore Slayer//EN
				CALSCALE:GREGORIAN
				METHOD:PUBLISH
				BEGIN:VEVENT
				UID:oob-quest-%d@bluesix.dev
				DTSTAMP:%s
				DTSTART:%s
				DURATION:PT%dM
				SUMMARY:⚔️ Quest: %s
				DESCRIPTION:%s\\n\\nMicro-Steps:\\n- %s
				STATUS:CONFIRMED
				END:VEVENT
				END:VCALENDAR
				""".formatted(
						thing.getId(),
						now,
						now,
						thing.getEstimatedMinutes(),
						escapeIcs(thing.getOriginal()),
						escapeIcs(twistText),
						escapeIcs(steps)
				);
	}

	@Transactional(readOnly = true)
	public QuestOverview getOverview(Player player, Lang lang) {
		List<BoringThing> playerThings = player.isTracked()
				? things.findByPlayerIdOrderByCreatedAtDesc(player.id())
				: List.of();

		List<ThingView> myActive = playerThings.stream()
				.filter(t -> !t.isSlayed())
				.map(t -> ThingView.of(t, lang))
				.toList();

		List<ThingView> mySlayed = playerThings.stream()
				.filter(BoringThing::isSlayed)
				.map(t -> ThingView.of(t, lang))
				.toList();

		List<ThingView> recentHall = things.findTop20BySlayedAtIsNotNullOrderBySlayedAtDesc().stream()
				.map(t -> ThingView.of(t, lang))
				.toList();

		int slayed = achievements.slayedCount(player.id());
		int xp = achievements.slayerXp(player.id());
		int level = 1 + (xp / 200);

		return new QuestOverview(myActive, mySlayed, recentHall, slayed, xp, level);
	}

	private static String escapeIcs(String text) {
		return text.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace("\n", "\\n");
	}

}
