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

import dev.oob.chaos.board.ChaosMeter;
import dev.oob.chaos.board.ChaosService;
import dev.oob.chaos.board.ThingView;
import dev.oob.chaos.live.ChaosBroadcaster;
import dev.oob.chaos.live.ChaosEvent;

@RestController
@RequestMapping("/api")
class ChaosController {

	private final ChaosService chaos;

	private final ChaosBroadcaster broadcaster;

	ChaosController(ChaosService chaos, ChaosBroadcaster broadcaster) {
		this.chaos = chaos;
		this.broadcaster = broadcaster;
	}

	@GetMapping("/things")
	List<ThingView> board() {
		return chaos.board();
	}

	@PostMapping("/things")
	ResponseEntity<ThingView> submit(@Valid @RequestBody SubmitRequest request) {
		ThingView created = chaos.submit(request.original(), request.author());
		return ResponseEntity.created(URI.create("/api/things/" + created.id())).body(created);
	}

	@PostMapping("/things/{id}/hype")
	ThingView hype(@PathVariable long id) {
		return chaos.hype(id);
	}

	@PostMapping("/things/{id}/reroll")
	ThingView reroll(@PathVariable long id) {
		return chaos.reroll(id);
	}

	@GetMapping("/meter")
	ChaosMeter meter() {
		return chaos.meter();
	}

	@GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	SseEmitter stream() {
		return broadcaster.subscribe(List.of(ChaosEvent.board(chaos.board()), ChaosEvent.meter(chaos.meter())));
	}

}
