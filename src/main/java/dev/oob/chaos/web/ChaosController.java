package dev.oob.chaos.web;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import dev.oob.chaos.achievements.AchievementTracker;
import dev.oob.chaos.achievements.AchievementView;
import dev.oob.chaos.board.ChaosMeter;
import dev.oob.chaos.board.ChaosService;
import dev.oob.chaos.board.ThingView;
import dev.oob.chaos.i18n.Lang;
import dev.oob.chaos.live.ChaosBroadcaster;
import dev.oob.chaos.live.ChaosEvent;
import dev.oob.chaos.live.Chronicle;
import dev.oob.chaos.mutator.MutatorDirector;
import dev.oob.chaos.player.Player;

@RestController
@RequestMapping("/api")
class ChaosController {

	private final ChaosService chaos;

	private final ChaosBroadcaster broadcaster;

	private final AchievementTracker achievements;

	private final MutatorDirector mutators;

	private final Chronicle chronicle;

	ChaosController(ChaosService chaos, ChaosBroadcaster broadcaster, AchievementTracker achievements,
			MutatorDirector mutators, Chronicle chronicle) {
		this.chaos = chaos;
		this.broadcaster = broadcaster;
		this.achievements = achievements;
		this.mutators = mutators;
		this.chronicle = chronicle;
	}

	@GetMapping("/things")
	List<ThingView> board(Lang lang) {
		return chaos.board().in(lang);
	}

	@PostMapping("/things")
	ResponseEntity<ThingView> submit(@Valid @RequestBody SubmitRequest request, Player player, Lang lang) {
		ThingView created = chaos.submit(request.original(), request.author(), request.genre(), player, lang);
		return ResponseEntity.created(URI.create("/api/things/" + created.id())).body(created);
	}

	@PostMapping("/things/{id}/hype")
	ThingView hype(@PathVariable long id, Player player, Lang lang) {
		return chaos.hype(id, player, lang);
	}

	@PostMapping("/things/{id}/reroll")
	ThingView reroll(@PathVariable long id, Player player, Lang lang) {
		return chaos.reroll(id, player, lang);
	}

	@GetMapping("/meter")
	ChaosMeter meter(Lang lang) {
		return chaos.meter().in(lang);
	}

	@GetMapping("/achievements")
	List<AchievementView> achievements(Player player, Lang lang) {
		return achievements.forPlayer(player, lang);
	}

	/** Open with {@code ?lang=en} for English; everything on this stream then arrives in that language. */
	@GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	SseEmitter stream(Lang lang) {
		return broadcaster.subscribe(lang, List.of(
				ChaosEvent.board(chaos.board()),
				ChaosEvent.meter(chaos.meter()),
				new ChaosEvent("mutator", mutators.state()),
				new ChaosEvent("chronicle", chronicle.recent())));
	}

}
