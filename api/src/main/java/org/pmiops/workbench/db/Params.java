package org.pmiops.workbench.db;

import com.zaxxer.hikari.HikariConfig;
import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.util.Optional;
import java.util.logging.Logger;
import org.springframework.context.annotation.Configuration;

// @Configuration means, "Provide this as an injectable dependency."
// This is a singleton, which means that the load is done only once and, more importantly, the
// logging only happens once.
@Configuration
public class Params {
  private static final Logger log = Logger.getLogger(Params.class.getName());

  public static final int mysqlDefaultPort = 3306;
  public String hostname;
  public final String username = "workbench"; // consistent across environments
  public String cloudSqlInstanceName;
  public String password;
  private boolean loaded;

  public Params() {
    loadFromEnvironment();
    validate();
    logParams();
  }

  public void loadFromEnvironment() {
    hostname = getEnv("DB_HOST").orElse(null);
    cloudSqlInstanceName = getEnv("CLOUD_SQL_INSTANCE_NAME").orElse(null);
    password = getEnv("WORKBENCH_DB_PASSWORD").orElse(null);
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
    return config;
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
      if (!getEnv("GOOGLE_APPLICATION_CREDENTIALS").isPresent()
          && !getEnv("GAE_INSTANCE").isPresent()) {
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

  public static Optional<String> getEnv(String name) {
    return Optional.ofNullable(System.getenv(name))
        .map(String::trim)
        .filter(s -> !s.equals(""))
        // kluge to work around the issue of translating empty env vars to the env var name
        // Actual use case: empty CDR_CLOUD_SQL_INSTANCE_NAME might resolve to the *literal*
        // string "${CDR_CLOUD_SQL_INSTANCE_NAME}" instead of expanding it
        .filter(s -> !s.contains(name));
  }
}
