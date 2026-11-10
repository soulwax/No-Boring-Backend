package dev.oob.chaos.auth;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.oob.chaos.player.Player;

@RestController
class AccountController {

	/**
	 * Who the server thinks you are.
	 *
	 * @param tag the handle your achievement broadcasts carry, so the browser can spot its own
	 * @param signInUrl where to start a GitHub sign-in, or {@code null} when it isn't configured
	 */
	record Account(boolean signedIn, String name, @Nullable String login, @Nullable String avatarUrl,
			@Nullable String tag, @Nullable String signInUrl) {
	}

	private final boolean gitHub;

	AccountController(ObjectProvider<ClientRegistrationRepository> clients) {
		this.gitHub = clients.getIfAvailable() != null;
	}

	@GetMapping("/api/me")
	Account me(Player player) {
		String signIn = gitHub ? SecurityConfig.SIGN_IN + "/github" : null;
		return GitHubAccount.current()
			.map(a -> new Account(true, player.name(), a.login(), a.avatarUrl(), player.tag(), signIn))
			.orElseGet(() -> new Account(false, player.name(), null, null, player.tag(), signIn));
	}

}
