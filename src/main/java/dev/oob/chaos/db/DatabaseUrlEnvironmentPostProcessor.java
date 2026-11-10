package dev.oob.chaos.db;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Turns the libpq-style URLs that Neon (and Vercel's Neon integration) hand out into Spring
 * configuration, so {@code DATABASE_URL} alone switches the app from in-memory H2 to Postgres:
 * <ul>
 * <li>{@code DATABASE_URL} (pooled, through PgBouncer) serves the application's connection pool;</li>
 * <li>{@code DATABASE_URL_UNPOOLED} (direct) runs Flyway and holds the {@code LISTEN} connection,
 * neither of which survives transaction pooling.</li>
 * </ul>
 * Runs after config data is loaded, so a {@code .env} imported by {@code spring.config.import} counts.
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

	static final String SOURCE = "chaosDatabaseUrl";

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment env, SpringApplication application) {
		if (!env.getProperty("chaos.database.enabled", Boolean.class, true)) {
			return;
		}
		Connection pooled = Connection.parse(env.getProperty("DATABASE_URL"));
		if (pooled == null) {
			return;
		}
		Connection direct = Connection.parse(env.getProperty("DATABASE_URL_UNPOOLED"));
		if (direct == null) {
			direct = pooled;
		}
		Map<String, Object> props = new LinkedHashMap<>();
		props.put("spring.datasource.url", pooled.jdbcUrl());
		props.put("spring.datasource.username", pooled.user());
		props.put("spring.datasource.password", pooled.password());
		props.put("spring.flyway.url", direct.jdbcUrl());
		props.put("spring.flyway.user", direct.user());
		props.put("spring.flyway.password", direct.password());
		props.put("chaos.database.direct-url", direct.jdbcUrl());
		props.put("chaos.database.direct-username", direct.user());
		props.put("chaos.database.direct-password", direct.password());
		props.put("chaos.relay.mode", "postgres");
		// The console could reach any JDBC URL; it has no business running next to a real database
		props.put("spring.h2.console.enabled", "false");
		env.getPropertySources().addFirst(new MapPropertySource(SOURCE, props));
	}

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}

	/** One parsed {@code postgres[ql]://user:password@host[:port]/db?params} URL. */
	record Connection(String jdbcUrl, String user, String password) {

		/** libpq parameter names that pgjdbc spells differently */
		private static final Map<String, String> RENAMED = Map.of("channel_binding", "channelBinding");

		static @Nullable Connection parse(@Nullable String url) {
			if (url == null || url.isBlank()) {
				return null;
			}
			URI uri = URI.create(url.strip());
			if (!"postgres".equals(uri.getScheme()) && !"postgresql".equals(uri.getScheme())) {
				throw new IllegalArgumentException("Expected a postgres:// or postgresql:// URL, got " + uri.getScheme());
			}
			String userInfo = uri.getRawUserInfo() == null ? "" : uri.getRawUserInfo();
			int colon = userInfo.indexOf(':');
			String user = decode(colon < 0 ? userInfo : userInfo.substring(0, colon));
			String password = colon < 0 ? "" : decode(userInfo.substring(colon + 1));
			String host = uri.getPort() < 0 ? uri.getHost() : uri.getHost() + ":" + uri.getPort();
			return new Connection("jdbc:postgresql://" + host + uri.getRawPath() + query(uri.getRawQuery()), user,
					password);
		}

		private static String query(@Nullable String raw) {
			if (raw == null || raw.isEmpty()) {
				return "";
			}
			StringJoiner params = new StringJoiner("&", "?", "");
			for (String pair : raw.split("&")) {
				int eq = pair.indexOf('=');
				String key = eq < 0 ? pair : pair.substring(0, eq);
				params.add(RENAMED.getOrDefault(key, key) + (eq < 0 ? "" : pair.substring(eq)));
			}
			return params.toString();
		}

		private static String decode(String part) {
			return URLDecoder.decode(part, StandardCharsets.UTF_8);
		}

		@Override
		public String toString() {
			return "Connection[" + jdbcUrl + ", user=" + user + "]";
		}

	}

}
