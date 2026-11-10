package dev.oob.chaos.auth;

import java.util.Arrays;
import java.util.Set;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * The room is open to everyone; signing in with GitHub only gives you a name and trophies that
 * follow you across devices. Every endpoint stays public, the GitHub flow lives under
 * {@code /api/auth} (so the same proxy rules that reach the API reach it), and CSRF protection
 * applies exactly where a cookie could be abused: state-changing requests that carry a session.
 */
@Configuration
class SecurityConfig {

	static final String SIGN_IN = "/api/auth/signin";

	static final String SESSION_COOKIE = "SESSION";

	private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");

	/**
	 * Anonymous calls (curl, the e2e checks, a guest's browser) carry no session and need no
	 * token. A signed-in browser does: Angular echoes the XSRF-TOKEN cookie as X-XSRF-TOKEN.
	 */
	static final RequestMatcher SESSION_BOUND_WRITES = request -> !SAFE_METHODS.contains(request.getMethod())
			&& hasSessionCookie(request) && !request.getRequestURI().startsWith("/h2-console");

	@Bean
	SecurityFilterChain room(HttpSecurity http, ObjectProvider<ClientRegistrationRepository> clients,
			AccountMerger merger) throws Exception {
		http.authorizeHttpRequests(requests -> requests.anyRequest().permitAll())
			.csrf(csrf -> csrf.spa().requireCsrfProtectionMatcher(SESSION_BOUND_WRITES))
			// The H2 console renders in frames; nothing else here does
			.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
			.formLogin(form -> form.disable())
			.httpBasic(basic -> basic.disable())
			.requestCache(cache -> cache.disable())
			.logout(logout -> logout.logoutUrl("/api/auth/signout")
				.logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)));

		ClientRegistrationRepository registrations = clients.getIfAvailable();
		if (registrations != null) {
			http.oauth2Login(login -> login
				.authorizationEndpoint(endpoint -> endpoint
					.authorizationRequestResolver(new GuestRememberingResolver(registrations, SIGN_IN)))
				.redirectionEndpoint(endpoint -> endpoint.baseUri(GitHubLoginEnvironmentPostProcessor.CALLBACK_PATH + "*"))
				.successHandler(new GitHubSignInSuccessHandler(merger))
				.failureUrl("/?signInFailed"));
		}
		return http.build();
	}

	private static boolean hasSessionCookie(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		return cookies != null && Arrays.stream(cookies).anyMatch(c -> SESSION_COOKIE.equals(c.getName()));
	}

}
