package dev.oob.chaos.auth;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Registers GitHub as an OAuth2 client only when {@code GITHUB_CLIENT_ID} and
 * {@code GITHUB_CLIENT_SECRET} are present, so the app still starts (and plays anonymously)
 * without them. {@code GITHUB_CALLBACK_URL} overrides where GitHub sends people back to; it must
 * match the callback configured on GitHub's side.
 */
public class GitHubLoginEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

	static final String PREFIX = "spring.security.oauth2.client.registration.github.";

	static final String CALLBACK_PATH = "/api/auth/callback/";

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment env, SpringApplication application) {
		String id = env.getProperty("GITHUB_CLIENT_ID", "").strip();
		String secret = env.getProperty("GITHUB_CLIENT_SECRET", "").strip();
		if (!env.getProperty("chaos.auth.github.enabled", Boolean.class, true) || id.isEmpty() || secret.isEmpty()) {
			return;
		}
		String callback = env.getProperty("GITHUB_CALLBACK_URL", "").strip();
		Map<String, Object> props = new LinkedHashMap<>();
		props.put(PREFIX + "client-id", id);
		props.put(PREFIX + "client-secret", secret);
		props.put(PREFIX + "scope", "read:user");
		props.put(PREFIX + "redirect-uri",
				callback.isEmpty() ? "{baseUrl}" + CALLBACK_PATH + "{registrationId}" : callback);
		env.getPropertySources().addFirst(new MapPropertySource("chaosGitHubLogin", props));
	}

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}

}
