package dev.oob.chaos.web;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import dev.oob.chaos.i18n.Lang;

/**
 * Lets any controller method simply declare a {@link Lang} parameter. A {@code ?lang=} query
 * parameter wins (EventSource can't set headers), then {@code Accept-Language}, then German.
 */
class LangArgumentResolver implements HandlerMethodArgumentResolver {

	static final String PARAM = "lang";

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.getParameterType() == Lang.class;
	}

	@Override
	public Lang resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest request, WebDataBinderFactory binderFactory) {
		return resolve(request);
	}

	static Lang resolve(NativeWebRequest request) {
		String param = request.getParameter(PARAM);
		return param != null ? Lang.parse(param) : Lang.of(LocaleContextHolder.getLocale());
	}

}
