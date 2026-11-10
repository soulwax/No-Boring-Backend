package dev.oob.chaos.web;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;

@SpringBootTest(properties = { "chaos.decay-interval=1h", "chaos.seed-demo-data=false" })
@AutoConfigureMockMvc
class ChaosApiTest {

	@Autowired
	MockMvc mvc;

	@Test
	void submittingABoringThingReturnsItTwisted() throws Exception {
		mvc.perform(post("/api/things").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"original": "sorting receipts", "author": "Tester"}"""))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", startsWith("/api/things/")))
			.andExpect(jsonPath("$.twist").value(startsWith("Sorting receipts ")))
			.andExpect(jsonPath("$.author").value("Tester"))
			.andExpect(jsonPath("$.hype").value(0));
	}

	@Test
	void funSubmissionsAreRejectedWithAProblemDetail() throws Exception {
		mvc.perform(post("/api/things").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"original": "skydiving into a volcano!!"}"""))
			.andExpect(status().isBadRequest())
			.andExpect(header().string("Content-Type", "application/problem+json"))
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
	void unknownThingIs404Problem() throws Exception {
		mvc.perform(post("/api/things/{id}/hype", 999_999))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.title").value("This boring thing escaped"));
	}

}
