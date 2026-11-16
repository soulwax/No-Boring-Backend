package dev.oob.chaos.quest;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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

@SpringBootTest(properties = {
		"chaos.decay-interval=1h",
		"chaos.mutator-interval=1h",
		"chaos.seed-demo-data=false"
})
@AutoConfigureMockMvc
class QuestApiTest {

	@Autowired
	MockMvc mvc;

	@Test
	void submitEnrichesWithQuestStepsAndAllowsSlaying() throws Exception {
		// 1. Submit chore
		MvcResult submitResult = mvc.perform(post("/api/things")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"original": "Steuererklaerung einreichen", "genre": "NOIR"}"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.questSteps", notNullValue()))
				.andExpect(jsonPath("$.estimatedMinutes").isNumber())
				.andReturn();

		int id = JsonPath.read(submitResult.getResponse().getContentAsString(), "$.id");

		// 2. Download .ics Calendar event
		mvc.perform(get("/api/things/" + id + "/calendar.ics"))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Type", containsString("text/calendar")))
				.andExpect(content().string(containsString("BEGIN:VCALENDAR")))
				.andExpect(content().string(containsString("Steuererklaerung einreichen")));

		// 3. Slay the chore
		mvc.perform(post("/api/things/" + id + "/slay"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.slayedAt", notNullValue()));

		// 4. Verify quest overview
		mvc.perform(get("/api/quests"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.slayedCount").isNumber())
				.andExpect(jsonPath("$.slayerXp").isNumber())
				.andExpect(jsonPath("$.slayerLevel").isNumber());
	}

}
