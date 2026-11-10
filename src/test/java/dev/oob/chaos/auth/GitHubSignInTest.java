package dev.oob.chaos.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.OAuth2LoginRequestPostProcessor;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.test.web.servlet.MockMvc;

import dev.oob.chaos.player.Player;

@SpringBootTest(properties = { "chaos.auth.github.enabled=true", "GITHUB_CLIENT_ID=test-client",
		"GITHUB_CLIENT_SECRET=test-secret", "GITHUB_CALLBACK_URL=https://excite.example/api/auth/callback/github",
		"chaos.decay-interval=1h", "chaos.mutator-interval=1h", "chaos.seed-demo-data=false" })
@AutoConfigureMockMvc
class GitHubSignInTest {

	private static final ClientRegistration GITHUB = CommonOAuth2Provider.GITHUB.getBuilder("github")
		.clientId("test-client")
		.clientSecret("test-secret")
		.build();

	@Autowired
	MockMvc mvc;

	@Autowired
	AccountMerger merger;

	@Autowired
	SessionRepository<? extends Session> sessions;

	@Test
	void signingInRedirectsToGitHubWithTheConfiguredCallbackAndRemembersTheGuest() throws Exception {
		Cookie session = mvc.perform(get("/api/auth/signin/github").param("guest", "guest-1234-5678"))
			.andExpect(status().is3xxRedirection())
			.andExpect(header().string("Location", startsWith("https://github.com/login/oauth/authorize?")))
			.andExpect(header().string("Location",
					containsString("redirect_uri=https://excite.example/api/auth/callback/github")))
			.andReturn()
			.getResponse()
			.getCookie(SecurityConfig.SESSION_COOKIE);

		// Stored in the database, so the instance GitHub calls back may well be another one
		String id = new String(Base64.getDecoder().decode(session.getValue()), StandardCharsets.UTF_8);
		assertThat(sessions.findById(id).<String>getAttribute(GuestRememberingResolver.GUEST))
			.isEqualTo("guest-1234-5678");
	}

	@Test
	void guestsSeeTheSignInOptionAndKeepTheirHeaderIdentity() throws Exception {
		mvc.perform(get("/api/me").header("X-Chaos-Name", "Ada"))
			.andExpect(jsonPath("$.signedIn").value(false))
			.andExpect(jsonPath("$.name").value("Ada"))
			.andExpect(jsonPath("$.signInUrl").value("/api/auth/signin/github"));
	}

	@Test
	void aSignedInPlayerIsTheirGitHubAccountWhateverTheHeadersSay() throws Exception {
		mvc.perform(get("/api/me").with(octocat()).header("X-Chaos-Player", "someone-elses-id"))
			.andExpect(jsonPath("$.signedIn").value(true))
			.andExpect(jsonPath("$.name").value("The Octocat"))
			.andExpect(jsonPath("$.login").value("octocat"))
			.andExpect(jsonPath("$.tag").value(new Player("github-583231", "x").tag()));

		mvc.perform(post("/api/things").with(octocat()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"original": "Formulare ausfüllen"}"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.author").value("The Octocat"));
	}

	@Test
	void csrfGuardsSessionBoundWritesOnly() throws Exception {
		String json = """
				{"original": "Quittungen abheften"}""";
		mvc.perform(post("/api/things").contentType(MediaType.APPLICATION_JSON).content(json))
			.andExpect(status().isCreated());
		mvc.perform(post("/api/things").cookie(new Cookie(SecurityConfig.SESSION_COOKIE, "stolen"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpect(status().isForbidden());
		mvc.perform(post("/api/things").cookie(new Cookie(SecurityConfig.SESSION_COOKIE, "mine")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpect(status().isCreated());
	}

	@Test
	void signingInAdoptsTheGuestsTrophies() throws Exception {
		String guest = "guest-" + UUID.randomUUID();
		mvc.perform(post("/api/things").header("X-Chaos-Player", guest).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"original": "Pfandflaschen zurückbringen"}"""))
			.andExpect(status().isCreated());

		merger.adopt(guest, new Player("github-583231", "The Octocat"));

		mvc.perform(get("/api/achievements").with(octocat()))
			.andExpect(jsonPath("$[?(@.code == 'BOREDOM_SLAYER')].unlocked").value(true));
		mvc.perform(get("/api/achievements").header("X-Chaos-Player", guest))
			.andExpect(jsonPath("$[?(@.code == 'BOREDOM_SLAYER')].unlocked").value(false));
	}

	@Test
	void signingOutEndsTheSession() throws Exception {
		mvc.perform(post("/api/auth/signout").with(octocat()).with(csrf())).andExpect(status().isNoContent());
	}

	private static OAuth2LoginRequestPostProcessor octocat() {
		return oauth2Login().clientRegistration(GITHUB)
			.attributes(a -> a.putAll(Map.of("id", 583231, "login", "octocat", "name", "The Octocat", "avatar_url",
					"https://avatars.githubusercontent.com/u/583231")));
	}

}
