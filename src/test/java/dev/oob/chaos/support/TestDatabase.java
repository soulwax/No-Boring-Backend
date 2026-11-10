package dev.oob.chaos.support;

import java.util.UUID;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;

/** A fresh, migrated in-memory database per call, in the same PostgreSQL mode the app uses. */
public final class TestDatabase {

	private TestDatabase() {
	}

	public static DataSource migrated() {
		JdbcDataSource ds = new JdbcDataSource();
		ds.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
		Flyway.configure().dataSource(ds).load().migrate();
		return ds;
	}

}
