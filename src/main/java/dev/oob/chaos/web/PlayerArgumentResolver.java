package dev.oob.chaos.web;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import dev.oob.chaos.auth.GitHubAccount;
import dev.oob.chaos.player.Player;

/**
 * Lets any controller method simply declare a {@link Player} parameter. A GitHub sign-in wins;
 * otherwise the guest is built from the {@code X-Chaos-Player} / {@code X-Chaos-Name} headers
 * the Angular interceptor adds.
 */
class PlayerArgumentResolver implements HandlerMethodArgumentResolver {

	static final String ID_HEADER = "X-Chaos-Player";

	static final String NAME_HEADER = "X-Chaos-Name";

	private static final Pattern VALID_ID = Pattern.compile("[A-Za-z0-9-]{8,64}");

	private static final int MAX_NAME = 40;

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.getParameterType() == Player.class;
	}

	@Override
	public Player resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest request, WebDataBinderFactory binderFactory) {
		var account = GitHubAccount.current();
		if (account.isPresent()) {
			return new Player(account.get().playerId(), account.get().displayName());
		}
		String id = request.getHeader(ID_HEADER);
		String name = name(request.getHeader(NAME_HEADER), Player.defaultName(LangArgumentResolver.resolve(request)));
		if (id == null || !VALID_ID.matcher(id).matches()) {
			return new Player(null, name);
		}
		return new Player(id, name);
	}

	private static String name(String header, String fallback) {
		if (header == null) {
			return fallback;
		}
		String decoded;
		try {
			decoded = URLDecoder.decode(header, StandardCharsets.UTF_8).strip();
		}
		catch (IllegalArgumentException ex) {
			return fallback;
		}
		if (decoded.isEmpty()) {
			return fallback;
		}
		return decoded.length() > MAX_NAME ? decoded.substring(0, MAX_NAME) : decoded;
	}

}
