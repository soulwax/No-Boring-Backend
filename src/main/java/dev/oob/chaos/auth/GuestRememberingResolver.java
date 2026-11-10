package dev.oob.chaos.auth;

import jakarta.servlet.http.HttpServletRequest;

import org.jspecify.annotations.Nullable;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

/**
 * Starts the GitHub redirect from {@code /api/auth/signin/github?guest=<id>} and keeps the
 * browser's guest id in the session, so {@link GitHubSignInSuccessHandler} can hand that
 * guest's progress to the account once GitHub sends the player back.
 */
class GuestRememberingResolver implements OAuth2AuthorizationRequestResolver {

	static final String GUEST = "chaos.guest";

	private final OAuth2AuthorizationRequestResolver delegate;

	GuestRememberingResolver(ClientRegistrationRepository clients, String baseUri) {
		this.delegate = new DefaultOAuth2AuthorizationRequestResolver(clients, baseUri);
	}

	@Override
	public @Nullable OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
		return remember(request, delegate.resolve(request));
	}

	@Override
	public @Nullable OAuth2AuthorizationRequest resolve(HttpServletRequest request, String registrationId) {
		return remember(request, delegate.resolve(request, registrationId));
	}

	private static @Nullable OAuth2AuthorizationRequest remember(HttpServletRequest request,
			@Nullable OAuth2AuthorizationRequest authorization) {
		String guest = request.getParameter("guest");
		if (authorization != null && guest != null && AccountMerger.isGuestId(guest)) {
			request.getSession().setAttribute(GUEST, guest);
		}
		return authorization;
	}

}
