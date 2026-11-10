package dev.oob.chaos.web;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;

import dev.oob.chaos.mutator.Mutator;
import dev.oob.chaos.mutator.MutatorDirector;
import dev.oob.chaos.twist.Genre;

@SpringBootTest(properties = { "chaos.decay-interval=1h", "chaos.mutator-interval=1h", "chaos.seed-demo-data=false" })
@AutoConfigureMockMvc
class ChaosApiTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	MutatorDirector mutators;

	/** Waves start mutators; never let one leak into the next test. */
	@AfterEach
	void calmDown() {
		mutators.stop();
	}

	@Test
	void submittingABoringThingReturnsItTwistedInGermanByDefault() throws Exception {
		mvc.perform(post("/api/things").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"original": "Belege sortieren", "genre": "CLASSIC"}"""))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", startsWith("/api/things/")))
			.andExpect(jsonPath("$.twist").value(startsWith("Belege sortieren – ")))
			.andExpect(jsonPath("$.author").value("Anonyme Legende"))
			.andExpect(jsonPath("$.hype").value(0));
	}

	@Test
	void englishIsOneHeaderOrQueryParameterAway() throws Exception {
		Integer id = submit("""
				{"original": "sorting receipts", "author": "Tester", "genre": "NOIR"}""");

		mvc.perform(post("/api/things/{id}/reroll", id).header(HttpHeaders.ACCEPT_LANGUAGE, "en-US,en;q=0.9"))
			.andExpect(jsonPath("$.twist").value(startsWith("It was raining again. Sorting receipts ")))
			.andExpect(jsonPath("$.author").value("Tester"));
		mvc.perform(post("/api/things/{id}/reroll", id).param("lang", "en").header(HttpHeaders.ACCEPT_LANGUAGE, "de"))
			.andExpect(jsonPath("$.twist").value(startsWith("It was raining again. ")));
		mvc.perform(get("/api/meter").header(HttpHeaders.ACCEPT_LANGUAGE, "fr"))
			.andExpect(jsonPath("$.mood").value("Gähnende Leere"));
		mvc.perform(get("/api/achievements").param("lang", "en"))
			.andExpect(jsonPath("$[0].title").value("Boredom Slayer"));
	}

	@Test
	void funSubmissionsAreRejectedWithAProblemDetail() throws Exception {
		mvc.perform(post("/api/things").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"original": "Fallschirmspringen in einen Vulkan!!"}"""))
			.andExpect(status().isBadRequest())
			.andExpect(header().string("Content-Type", "application/problem+json"))
			.andExpect(jsonPath("$.title").value("Validierung fehlgeschlagen"))
			.andExpect(jsonPath("$.errors.original").value(startsWith("Das klingt schon nach Spaß")));

		mvc.perform(post("/api/things").contentType(MediaType.APPLICATION_JSON)
				.header(HttpHeaders.ACCEPT_LANGUAGE, "en")
				.content("""
						{"original": "skydiving into a volcano!!"}"""))
			.andExpect(jsonPath("$.errors.original").value(startsWith("That already sounds fun")));
	}

	@Test
	void hypeAddsThreeAndRerollCountsUp() throws Exception {
		MvcResult created = mvc.perform(post("/api/things").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"original": "renewing a parking permit"}"""))
			.andReturn();
		Integer id = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

		mvc.perform(post("/api/things/{id}/hype", id)).andExpect(jsonPath("$.hype").value(3));
		mvc.perform(post("/api/things/{id}/hype", id)).andExpect(jsonPath("$.hype").value(6));
		mvc.perform(post("/api/things/{id}/reroll", id)).andExpect(jsonPath("$.rerolls").value(1));
		mvc.perform(get("/api/meter")).andExpect(jsonPath("$.totalHype").isNumber());
	}

	@Test
	void playerHeadersEarnAchievementsIncludingTheChaosWave() throws Exception {
		String player = "test-player-0001";
		MvcResult created = mvc.perform(post("/api/things").contentType(MediaType.APPLICATION_JSON)
				.header(PlayerArgumentResolver.ID_HEADER, player)
				.header(PlayerArgumentResolver.NAME_HEADER, "Z%C3%B6e")
				.content("""
						{"original": "waiting on hold"}"""))
			.andExpect(jsonPath("$.author").value("Zöe"))
			.andReturn();
		Integer id = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

		// Enough hype to cross the wave threshold on its own
		for (int i = 0; i < 40; i++) {
			mvc.perform(post("/api/things/{id}/hype", id).header(PlayerArgumentResolver.ID_HEADER, player));
		}

		mvc.perform(get("/api/achievements").header(PlayerArgumentResolver.ID_HEADER, player))
			.andExpect(jsonPath("$[?(@.code == 'BOREDOM_SLAYER')].unlocked").value(true))
			.andExpect(jsonPath("$[?(@.code == 'SHAMELESS')].unlocked").value(true))
			.andExpect(jsonPath("$[?(@.code == 'HYPE_MACHINE')].unlocked").value(true))
			.andExpect(jsonPath("$[?(@.code == 'WAVE_RIDER')].unlocked").value(true))
			.andExpect(jsonPath("$[?(@.code == 'PLOT_TWIST_ADDICT')].unlocked").value(false));

		mvc.perform(get("/api/achievements"))
			.andExpect(jsonPath("$[?(@.unlocked == true)]").isEmpty());
	}

	@Test
	void achievementsComeInTwoApiVersions() throws Exception {
		String player = "test-player-0002";
		mvc.perform(post("/api/things").contentType(MediaType.APPLICATION_JSON)
				.header(PlayerArgumentResolver.ID_HEADER, player)
				.content("""
						{"original": "Socken sortieren"}"""))
			.andExpect(status().isCreated());

		// No header: version 1, the plain list it has always been
		mvc.perform(get("/api/achievements").header(PlayerArgumentResolver.ID_HEADER, player))
			.andExpect(jsonPath("$[0].code").value("BOREDOM_SLAYER"))
			.andExpect(jsonPath("$[0].unlocked").value(true));

		mvc.perform(get("/api/achievements").header(PlayerArgumentResolver.ID_HEADER, player)
				.header("API-Version", "2"))
			// At least one: run this after midnight and Insomniac joins in
			.andExpect(jsonPath("$.unlocked").value(greaterThanOrEqualTo(1)))
			.andExpect(jsonPath("$.total").value(11))
			.andExpect(jsonPath("$.trophies[?(@.code == 'BOREDOM_SLAYER')].unlockedAt").isNotEmpty())
			.andExpect(jsonPath("$.trophies[?(@.code == 'HYPE_MACHINE' && @.unlockedAt == null)]").isNotEmpty());

		mvc.perform(get("/api/achievements").header("API-Version", "7")).andExpect(status().isBadRequest());
	}

	@Test
	void genresShapeTheTwistAndHypeEvolvesTheThingWithLore() throws Exception {
		Integer id = submit("""
				{"original": "den Reisepass verlängern", "genre": "NOIR"}""");

		for (int i = 0; i < 2; i++) {
			mvc.perform(post("/api/things/{id}/hype", id));
		}
		mvc.perform(post("/api/things/{id}/hype", id))
			.andExpect(jsonPath("$.genre").value("NOIR"))
			.andExpect(jsonPath("$.twist").value(startsWith("Es regnete schon wieder. Den Reisepass verlängern – ")))
			.andExpect(jsonPath("$.hype").value(9))
			.andExpect(jsonPath("$.tier").value("CURIOUS"))
			.andExpect(jsonPath("$.peakTier").value("CURIOUS"))
			.andExpect(jsonPath("$.nextTierAt").value(15))
			.andExpect(jsonPath("$.lore.length()").value(1));
	}

	@Test
	void mutatorsBendTheRules() throws Exception {
		Integer id = submit("""
				{"original": "untangling cables"}""");

		mutators.start(Mutator.DOUBLE_HYPE, null);
		mvc.perform(post("/api/things/{id}/hype", id)).andExpect(jsonPath("$.hype").value(6));

		mutators.start(Mutator.GENRE_LOCK, Genre.HEIST);
		mvc.perform(post("/api/things").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"original": "matching socks", "genre": "NOIR"}"""))
			.andExpect(jsonPath("$.genre").value("HEIST"));

		mutators.start(Mutator.TWIST_TORNADO, null);
		mvc.perform(post("/api/things/{id}/hype", id))
			.andExpect(jsonPath("$.hype").value(9))
			.andExpect(jsonPath("$.rerolls").value(1));
	}

	@Test
	void unknownGenreIsABadRequest() throws Exception {
		mvc.perform(post("/api/things").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"original": "ironing", "genre": "MUSICAL"}"""))
			.andExpect(status().isBadRequest());
	}

	private Integer submit(String json) throws Exception {
		MvcResult created = mvc.perform(post("/api/things").contentType(MediaType.APPLICATION_JSON).content(json))
			.andExpect(status().isCreated())
			.andReturn();
		return JsonPath.read(created.getResponse().getContentAsString(), "$.id");
	}

	@Test
	void unknownThingIs404Problem() throws Exception {
		mvc.perform(post("/api/things/{id}/hype", 999_999))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.title").value("Dieses langweilige Ding ist entkommen"))
			.andExpect(jsonPath("$.detail").value(startsWith("Das langweilige Ding #999999 ")));
		mvc.perform(post("/api/things/{id}/hype", 999_999).header(HttpHeaders.ACCEPT_LANGUAGE, "en"))
			.andExpect(jsonPath("$.title").value("This boring thing escaped"));
	}

}
