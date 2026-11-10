package dev.oob.chaos.auth;

import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;

/**
 * The GitHub user behind the current request. Their player id is derived from GitHub's
 * numeric user id, which never changes (logins can be renamed).
 */
public record GitHubAccount(long githubId, String login, @Nullable String name, @Nullable String avatarUrl) {

	public static Optional<GitHubAccount> current() {
		return from(SecurityContextHolder.getContext().getAuthentication());
	}

	public static Optional<GitHubAccount> from(@Nullable Authentication authentication) {
		if (!(authentication instanceof OAuth2AuthenticationToken token)
				|| !"github".equals(token.getAuthorizedClientRegistrationId())) {
			return Optional.empty();
		}
		OAuth2User user = token.getPrincipal();
		Object id = user.getAttribute("id");
		if (!(id instanceof Number number)) {
			return Optional.empty();
		}
		return Optional.of(new GitHubAccount(number.longValue(), String.valueOf(user.<Object>getAttribute("login")),
				user.getAttribute("name"), user.getAttribute("avatar_url")));
	}

	public String playerId() {
		return "github-" + githubId;
	}

	/** What the room calls them: their profile name, else their login */
	public String displayName() {
		return name == null || name.isBlank() ? login : name.strip();
	}

}
