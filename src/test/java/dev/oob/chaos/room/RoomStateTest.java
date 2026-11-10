package dev.oob.chaos.room;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;

import dev.oob.chaos.support.TestDatabase;

class RoomStateTest {

	private final RoomState room = new RoomState(JdbcClient.create(TestDatabase.migrated()));

	@Test
	void exactlyOneOfManyRacingInstancesUnleashesTheWave() throws Exception {
		try (var threads = Executors.newVirtualThreadPerTaskExecutor()) {
			var attempts = IntStream.range(0, 32).<Callable<Boolean>>mapToObj(i -> room::disarmWave).toList();
			long winners = threads.invokeAll(attempts).stream().filter(f -> f.resultNow()).count();

			assertThat(winners).isEqualTo(1);
		}
		assertThat(room.disarmWave()).as("stays disarmed until rearmed").isFalse();
		room.rearmWave();
		assertThat(room.disarmWave()).isTrue();
	}

	@Test
	void eachEntropyTickIsClaimedOnce() {
		Instant tick = Instant.parse("2026-11-10T12:00:00Z");
		Duration interval = Duration.ofSeconds(4);

		assertThat(room.claimDecay(tick, interval)).isTrue();
		assertThat(room.claimDecay(tick.plusMillis(500), interval)).as("second instance, same tick").isFalse();
		assertThat(room.claimDecay(tick.plus(interval), interval)).as("next tick").isTrue();
	}

}
