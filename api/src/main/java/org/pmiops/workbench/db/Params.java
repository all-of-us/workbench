package org.pmiops.workbench.db;

import com.zaxxer.hikari.HikariConfig;
import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.util.logging.Logger;
import org.pmiops.workbench.config.EnvVars;
import org.springframework.context.annotation.Configuration;

// @Configuration means, "Provide this as an injectable dependency."
// This is a singleton, which means that the load is done only once and, more importantly, the
// logging only happens once.
@Configuration
public class Params {
  private static final Logger log = Logger.getLogger(Params.class.getName());

  protected final EnvVars envVars;
  public static final int mysqlDefaultPort = 3306;
  public String hostname;
  public final String username = "workbench"; // consistent across environments
  public String cloudSqlInstanceName;
  public String password;
  private boolean loaded;

  // Hikari pool sizing. Until this was added, createConfig set no sizing at all, so every pool
  // took Hikari's defaults -- and Hikari defaults minimumIdle to maximumPoolSize, meaning each
  // pool eagerly filled to 10 connections and never released one. Because CdrDataSource builds
  // one pool per CDR version, an app instance pinned (CDR versions + 1) * 10 connections open
  // regardless of traffic, so total usage scaled with instance count rather than with load. On
  // a MySQL instance with max_connections=4030 that ceiling is reached by roughly 36 instances,
  // and exceeding it fails every new connection server-wide (Connection_errors_max_connections).
  protected static final int DEFAULT_MAX_POOL_SIZE = 10;
  protected static final int DEFAULT_MIN_IDLE = 2;
  protected static final long IDLE_TIMEOUT_MS = 300_000; // 5 minutes
  protected static final long MAX_LIFETIME_MS = 1_500_000; // 25 minutes
  protected static final long KEEPALIVE_TIME_MS = 120_000; // 2 minutes

  public Params(EnvVars envVars) {
    this.envVars = envVars;
    loadFromEnvironment();
    validate();
    logParams();
  }

  public void loadFromEnvironment() {
    hostname = envVars.get("DB_HOST").orElse(null);
    cloudSqlInstanceName = envVars.get("CLOUD_SQL_INSTANCE_NAME").orElse(null);
    password = envVars.get("WORKBENCH_DB_PASSWORD").orElse(null);
  }

  protected void logParams() {
    log.info("Workbench SQL instance params: " + this.toString());
  }

  public HikariConfig createConfig(String dbName) {
    HikariConfig config = new HikariConfig();
    config.setDriverClassName("com.mysql.cj.jdbc.Driver");
    config.setJdbcUrl(
        String.format("jdbc:mysql://%s/%s", useAppEngineSocket() ? "" : hostname, dbName));
    config.setUsername("workbench"); // consistent across environments
    config.setPassword(password);
    if (useAppEngineSocket()) {
      config.addDataSourceProperty("socketFactory", "com.google.cloud.sql.mysql.SocketFactory");
      config.addDataSourceProperty("cloudSqlInstance", cloudSqlInstanceName);
    }
    // Named so that a pool can be attributed to its database in Hikari's logs and metrics, which
    // is how maximumPoolSize below should eventually be tuned (see applyPoolSizing).
    config.setPoolName("workbench-" + dbName);
    applyPoolSizing(config);
    return config;
  }

  /**
   * Pool sizing for the primary workbench database. This pool serves user-facing request paths, so
   * it keeps a small idle floor: with minimumIdle=0 the first request after a lull pays TCP, TLS,
   * MySQL auth and (on Cloud SQL) a token exchange.
   *
   * <p>Overridden by {@link org.pmiops.workbench.cdr.DbParams} for the per-CDR-version pools, which
   * are numerous and mostly idle and so want no floor at all.
   *
   * <p>maximumPoolSize is left at Hikari's existing default here deliberately: it caps concurrency,
   * and lowering it converts a slow query into a pool timeout for user requests. Tune it downward
   * from observed hikaricp_connections_active peaks, not from a guess.
   */
  protected void applyPoolSizing(HikariConfig config) {
    config.setMaximumPoolSize(intFromEnv("WORKBENCH_DB_MAX_POOL_SIZE", DEFAULT_MAX_POOL_SIZE));
    config.setMinimumIdle(intFromEnv("WORKBENCH_DB_MIN_IDLE", DEFAULT_MIN_IDLE));
    config.setIdleTimeout(IDLE_TIMEOUT_MS);
    config.setMaxLifetime(MAX_LIFETIME_MS);
    config.setKeepaliveTime(KEEPALIVE_TIME_MS);
  }

  /**
   * Reads an integer tuning knob from the environment, falling back to {@code defaultValue} when it
   * is unset or unparseable. Environment-driven so pool sizes can be adjusted per environment
   * without a code change.
   */
  protected int intFromEnv(String name, int defaultValue) {
    return envVars
        .get(name)
        .map(
            value -> {
              try {
                return Integer.parseInt(value.trim());
              } catch (NumberFormatException e) {
                log.warning(
                    String.format(
                        "Ignoring non-numeric %s=%s; using %d.", name, value, defaultValue));
                return defaultValue;
              }
            })
        .orElse(defaultValue);
  }

  public void validate() {
    if (hostname == null && cloudSqlInstanceName == null) {
      throw new IllegalStateException(
          "Database connection requires either a hostname (DB_HOST)"
              + " or a Cloud SQL instance name (CLOUD_SQL_INSTANCE_NAME).");
    }
    if (hostname != null) {
      try {
        new Socket(hostname, mysqlDefaultPort).close();
      } catch (ConnectException e) {
        throw new RuntimeException(
            String.format("Failed to connect to database on host %s.", hostname), e);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    } else {
      // assert cloudSqlInstanceName != null
      if (envVars.get("GOOGLE_APPLICATION_CREDENTIALS").isEmpty()
          && envVars.get("GAE_INSTANCE").isEmpty()) {
        throw new IllegalStateException(
            "Google Application Default Credentials are required to connect directly to Cloud SQL."
                + " Outside of App Engine, they can be provided with the environment variable"
                + " GOOGLE_APPLICATION_CREDENTIALS.");
      }
    }
  }

  public boolean useAppEngineSocket() {
    return hostname == null ? true : false;
  }

  @Override
  public String toString() {
    return String.format(
        "[hostname:%s cloudSqlInstanceName:%s username:%s password:%s]",
        hostname,
        cloudSqlInstanceName,
        username,
        // We wouldn't want to give a password hint in a public forum, but our logs are
        // relatively private.
        password != null ? shadow(2, password) : null);
  }

  private static String shadow(int visibleCount, String s) {
    return s.substring(0, visibleCount) + s.substring(visibleCount).replaceAll(".", "*");
  }
}
