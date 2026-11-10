package dev.oob.chaos.live;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Repository;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import dev.oob.chaos.i18n.Lang;
import dev.oob.chaos.i18n.Localized;

/**
 * The transactional outbox ({@code chaos_event}): every event with a payload is written here in
 * the same transaction as the change it describes, already rendered in every language. Readers
 * are the other instances (told about new rows by NOTIFY), the chronicle, and SSE clients that
 * reconnect with {@code Last-Event-ID}.
 */
@Repository
public class ChaosLog {

	/** One stored event; {@link #in(Lang)} picks the language a client reads. */
	public record Entry(long id, String type, JsonNode payload, Instant at) implements Localized<JsonNode> {

		@Override
		public JsonNode in(Lang lang) {
			return payload.get(key(lang));
		}

	}

	private static final Duration RETENTION = Duration.ofHours(1);

	/** The chronicle shows 20; keep a few more so late joiners never see a gap */
	private static final int KEEP_ACTIVITY = 50;

	private final JdbcClient jdbc;

	private final JsonMapper json;

	private final Clock clock;

	public ChaosLog(JdbcClient jdbc, JsonMapper json, Clock clock) {
		this.jdbc = jdbc;
		this.json = json;
		this.clock = clock;
	}

	/** Stores {@code payload} as every language reads it and returns the new event's id. */
	public long append(String type, Object payload) {
		Map<String, Object> perLang = new LinkedHashMap<>();
		for (Lang lang : Lang.values()) {
			perLang.put(key(lang), payload instanceof Localized<?> l ? l.in(lang) : payload);
		}
		KeyHolder keys = new GeneratedKeyHolder();
		jdbc.sql("insert into chaos_event (type, payload, created_at) values (:type, :payload, :at)")
			.param("type", type)
			.param("payload", json.writeValueAsString(perLang))
			.param("at", clock.instant().atOffset(ZoneOffset.UTC))
			.update(keys, "id");
		return keys.getKeyAs(Number.class).longValue();
	}

	public Optional<Entry> find(long id) {
		return jdbc.sql("select id, type, payload, created_at from chaos_event where id = :id")
			.param("id", id)
			.query(this::entry)
			.optional();
	}

	/** Events of {@code types} after {@code afterId}, oldest first. */
	public List<Entry> since(long afterId, Collection<String> types, int limit) {
		return jdbc.sql("""
				select id, type, payload, created_at from chaos_event
				where id > :after and type in (:types) order by id fetch first :limit rows only""")
			.param("after", afterId)
			.param("types", types)
			.param("limit", limit)
			.query(this::entry)
			.list();
	}

	/** The newest {@code limit} events of {@code type}, newest first. */
	public List<Entry> latest(String type, int limit) {
		return jdbc.sql("""
				select id, type, payload, created_at from chaos_event
				where type = :type order by id desc fetch first :limit rows only""")
			.param("type", type)
			.param("limit", limit)
			.query(this::entry)
			.list();
	}

	public long lastId() {
		return jdbc.sql("select coalesce(max(id), 0) from chaos_event").query(Long.class).single();
	}

	/** Old news goes; the newest activity stays, because it is the chronicle. Any instance may run it. */
	@Scheduled(fixedRate = 5, initialDelay = 1, timeUnit = TimeUnit.MINUTES)
	void prune() {
		jdbc.sql("delete from chaos_event where type <> 'activity' and created_at < :cutoff")
			.param("cutoff", clock.instant().minus(RETENTION).atOffset(ZoneOffset.UTC))
			.update();
		jdbc.sql("""
				delete from chaos_event where type = 'activity' and id < (
				  select coalesce(min(id), 0) from (
				    select id from chaos_event where type = 'activity' order by id desc fetch first :keep rows only
				  ) newest)""")
			.param("keep", KEEP_ACTIVITY)
			.update();
	}

	private Entry entry(ResultSet rs, int row) throws SQLException {
		return new Entry(rs.getLong("id"), rs.getString("type"), json.readTree(rs.getString("payload")),
				rs.getObject("created_at", OffsetDateTime.class).toInstant());
	}

	private static String key(Lang lang) {
		return lang.name().toLowerCase(Locale.ROOT);
	}

}
