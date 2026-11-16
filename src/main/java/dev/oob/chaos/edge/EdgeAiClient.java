package dev.oob.chaos.edge;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import dev.oob.chaos.i18n.Lang;
import dev.oob.chaos.twist.Genre;
import dev.oob.chaos.twist.TwistEngine;

/**
 * Connects to the Cloudflare Edge Worker for AI-assisted quest decomposition and Flux artwork generation.
 * Demonstrates Java 27 Virtual Threads with resilient local fallback to {@link TwistEngine}.
 */
@Service
public class EdgeAiClient {

	private static final Logger log = LoggerFactory.getLogger(EdgeAiClient.class);

	private final RestClient restClient;
	private final TwistEngine twistEngine;
	private final String edgeBaseUrl;
	private final ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();

	public EdgeAiClient(
			TwistEngine twistEngine,
			@Value("${chaos.edge.url:https://oob-edge-worker.imoen.workers.dev}") String edgeBaseUrl) {
		this.edgeBaseUrl = edgeBaseUrl;
		this.twistEngine = twistEngine;

		var requestFactory = new JdkClientHttpRequestFactory();
		requestFactory.setReadTimeout(Duration.ofSeconds(6));

		this.restClient = RestClient.builder()
				.baseUrl(edgeBaseUrl)
				.requestFactory(requestFactory)
				.build();
	}

	public record QuestEnhancement(
			String lore,
			String difficulty,
			int estimatedMinutes,
			List<String> microSteps,
			String imagePrompt) {}

	public record ArtworkResult(boolean success, String url, String key) {}

	public record EdgeQuestResponse(
			String lore,
			String difficulty,
			Integer estimatedMinutes,
			List<String> microSteps,
			String imagePrompt) {}

	public record EdgeArtResponse(
			Boolean success,
			String url,
			String key,
			String filename) {}

	/**
	 * Concurrently enhances a boring chore into an epic quest using Java 27 Virtual Threads.
	 * If the edge service fails or times out, it gracefully falls back without failing the user request.
	 */
	public QuestEnhancement enhanceQuest(String original, Genre genre, Lang lang) {
		try {
			CompletableFuture<QuestEnhancement> future = CompletableFuture.supplyAsync(
					() -> fetchFromEdge(original, genre, lang),
					virtualExecutor
			);

			// Fast timeout so player UI never feels sluggish
			QuestEnhancement result = future.get(3, TimeUnit.SECONDS);
			if (result != null && result.microSteps() != null && !result.microSteps().isEmpty()) {
				return result;
			}
		}
		catch (Exception ex) {
			log.info("Edge AI quest generation unavailable ({}), using local fallback", ex.getMessage());
		}

		return localFallback(original, genre, lang);
	}

	private QuestEnhancement fetchFromEdge(String original, Genre genre, Lang lang) {
		Map<String, Object> request = Map.of(
				"original", original,
				"genre", genre.name(),
				"lang", lang.name().toLowerCase(Locale.ROOT)
		);

		EdgeQuestResponse response = restClient.post()
				.uri("/api/edge/enhance-quest")
				.body(request)
				.retrieve()
				.body(EdgeQuestResponse.class);

		if (response == null) {
			return null;
		}

		String lore = response.lore() != null ? response.lore() : "";
		String difficulty = response.difficulty() != null ? response.difficulty() : "Rank C";
		int minutes = response.estimatedMinutes() != null ? response.estimatedMinutes() : 15;
		List<String> steps = response.microSteps() != null ? response.microSteps() : List.of();
		String prompt = response.imagePrompt() != null ? response.imagePrompt() : original;

		return new QuestEnhancement(lore, difficulty, minutes, steps, prompt);
	}

	/**
	 * Triggers Flux Schnell artwork generation in the Cloudflare Edge Worker and R2 bucket.
	 */
	public ArtworkResult generateArtwork(Long thingId, String prompt) {
		try {
			EdgeArtResponse response = restClient.post()
					.uri("/api/edge/generate-art")
					.body(Map.of("id", thingId, "prompt", prompt))
					.retrieve()
					.body(EdgeArtResponse.class);

			if (response != null && Boolean.TRUE.equals(response.success())) {
				String path = response.url();
				String fullUrl = edgeBaseUrl.replaceAll("/+$", "") + path;
				String key = response.key();
				return new ArtworkResult(true, fullUrl, key);
			}
		}
		catch (Exception e) {
			log.error("Failed to generate artwork via Cloudflare Edge: {}", e.getMessage());
		}
		return new ArtworkResult(false, null, null);
	}

	private QuestEnhancement localFallback(String original, Genre genre, Lang lang) {
		String lore = switch (genre) {
			case NOIR -> lang == Lang.DE ? "Der Regen trommelte gegen das Fenster, als der Plan geschmiedet wurde."
					: "Rain battered the blinds as the case file opened.";
			case SPACE_OPERA -> lang == Lang.DE ? "Der Hyperantrieb summte leise im Hintergrund."
					: "The hyperdrive hummed quietly in the background.";
			case MEDIEVAL -> lang == Lang.DE ? "Die königliche Schriftrolle wurde feierlich entrollt."
					: "The royal proclamation was solemnly unrolled.";
			case NATURE_DOC -> lang == Lang.DE ? "Hier beobachten wir das Subjekt in seinem natürlichen Lebensraum."
					: "Here we observe the subject in its natural habitat.";
			case HEIST -> lang == Lang.DE ? "Der Countdown läuft: Genau drei Phasen bis zum Ziel."
					: "The countdown ticks: exactly three phases to pull this off.";
			case CLASSIC -> lang == Lang.DE ? "Eine heroische Mission nimmt ihren Lauf."
					: "A heroic quest unfolds before the realm.";
		};

		List<String> steps = List.of(
				lang == Lang.DE ? "Phase 1: Vorbereitung und Fokussierung auf " + original : "Phase 1: Prepare and focus on " + original,
				lang == Lang.DE ? "Phase 2: Zügige Durchführung ohne Ablenkung" : "Phase 2: Swift execution without distractions",
				lang == Lang.DE ? "Phase 3: Triumphaler Abschluss und Feier" : "Phase 3: Triumphant completion and celebration"
		);

		String prompt = "Cinematic digital art illustrating the epic completion of " + original + " in " + genre.name() + " aesthetic";
		return new QuestEnhancement(lore, "Rank B", 20, steps, prompt);
	}

}
