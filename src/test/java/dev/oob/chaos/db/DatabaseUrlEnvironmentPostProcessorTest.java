package dev.oob.chaos.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

class DatabaseUrlEnvironmentPostProcessorTest {

	private static final String POOLED = "postgresql://neondb_owner:p%40ss@ep-x-pooler.eu-central-1.aws.neon.tech/neondb"
			+ "?channel_binding=require&sslmode=require";

	private static final String DIRECT = "postgres://neondb_owner:p%40ss@ep-x.eu-central-1.aws.neon.tech:5433/neondb"
			+ "?sslmode=require";

	@Test
	void neonUrlsBecomeAPooledDataSourceAndADirectConnectionForFlywayAndListen() {
		StandardEnvironment env = env(Map.of("DATABASE_URL", POOLED, "DATABASE_URL_UNPOOLED", DIRECT));

		new DatabaseUrlEnvironmentPostProcessor().postProcessEnvironment(env, new SpringApplication());

		assertThat(env.getProperty("spring.datasource.url")).isEqualTo(
				"jdbc:postgresql://ep-x-pooler.eu-central-1.aws.neon.tech/neondb?channelBinding=require&sslmode=require");
		assertThat(env.getProperty("spring.datasource.username")).isEqualTo("neondb_owner");
		assertThat(env.getProperty("spring.datasource.password")).as("percent-decoded").isEqualTo("p@ss");
		assertThat(env.getProperty("spring.flyway.url"))
			.isEqualTo("jdbc:postgresql://ep-x.eu-central-1.aws.neon.tech:5433/neondb?sslmode=require");
		assertThat(env.getProperty("chaos.database.direct-url")).isEqualTo(env.getProperty("spring.flyway.url"));
		assertThat(env.getProperty("chaos.relay.mode")).isEqualTo("postgres");
		assertThat(env.getProperty("spring.h2.console.enabled")).isEqualTo("false");
	}

	@Test
	void withoutAnUnpooledUrlThePooledOneServesEverything() {
		StandardEnvironment env = env(Map.of("DATABASE_URL", POOLED));

		new DatabaseUrlEnvironmentPostProcessor().postProcessEnvironment(env, new SpringApplication());

		assertThat(env.getProperty("spring.flyway.url")).isEqualTo(env.getProperty("spring.datasource.url"));
	}

	@Test
	void nothingChangesWithoutAUrlOrWhenSwitchedOff() {
		StandardEnvironment blank = env(Map.of("DATABASE_URL", " "));
		StandardEnvironment off = env(Map.of("DATABASE_URL", POOLED, "chaos.database.enabled", "false"));

		new DatabaseUrlEnvironmentPostProcessor().postProcessEnvironment(blank, new SpringApplication());
		new DatabaseUrlEnvironmentPostProcessor().postProcessEnvironment(off, new SpringApplication());

		assertThat(blank.getPropertySources().contains(DatabaseUrlEnvironmentPostProcessor.SOURCE)).isFalse();
		assertThat(off.getPropertySources().contains(DatabaseUrlEnvironmentPostProcessor.SOURCE)).isFalse();
	}

	@Test
	void theConnectionNeverPrintsItsPassword() {
		assertThat(DatabaseUrlEnvironmentPostProcessor.Connection.parse(POOLED)).hasToString(
				"Connection[jdbc:postgresql://ep-x-pooler.eu-central-1.aws.neon.tech/neondb?channelBinding=require&sslmode=require, user=neondb_owner]");
	}

	private static StandardEnvironment env(Map<String, Object> props) {
		StandardEnvironment env = new StandardEnvironment();
		env.getPropertySources().addFirst(new MapPropertySource("test", props));
		return env;
	}

}
