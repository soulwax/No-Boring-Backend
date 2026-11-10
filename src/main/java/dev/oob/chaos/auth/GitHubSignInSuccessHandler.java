package dev.oob.chaos.auth;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import dev.oob.chaos.player.Player;

/** Back from GitHub: adopt the guest's progress, then return to the board. */
class GitHubSignInSuccessHandler implements AuthenticationSuccessHandler {

	private static final Logger log = LoggerFactory.getLogger(GitHubSignInSuccessHandler.class);

	private final AccountMerger merger;

	GitHubSignInSuccessHandler(AccountMerger merger) {
		this.merger = merger;
	}

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException {
		HttpSession session = request.getSession(false);
		Object guest = session == null ? null : session.getAttribute(GuestRememberingResolver.GUEST);
		if (session != null) {
			session.removeAttribute(GuestRememberingResolver.GUEST);
		}
		GitHubAccount.from(authentication).ifPresent(account -> {
			if (guest instanceof String guestId) {
				try {
					merger.adopt(guestId, new Player(account.playerId(), account.displayName()));
				}
				catch (RuntimeException ex) {
					// Signing in matters more than carrying trophies over; never fail the login for it
					log.warn("Could not adopt guest progress for {}: {}", account.login(), ex.getMessage());
				}
			}
		});
		response.sendRedirect(request.getContextPath() + "/?signedIn");
	}

}
