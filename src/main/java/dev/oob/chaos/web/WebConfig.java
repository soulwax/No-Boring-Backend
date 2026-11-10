package dev.oob.chaos.web;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import dev.oob.chaos.i18n.Lang;

@Configuration
class WebConfig implements WebMvcConfigurer {

	/** German unless the request's {@code Accept-Language} prefers English. */
	@Bean
	LocaleResolver localeResolver() {
		AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
		resolver.setSupportedLocales(Lang.LOCALES);
		resolver.setDefaultLocale(Lang.DEFAULT.locale());
		return resolver;
	}

	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
		resolvers.add(new PlayerArgumentResolver());
		resolvers.add(new LangArgumentResolver());
	}

}
