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
  public String username = "workbench";
  public String cloudSqlInstanceName;
  public String password;
  public String iamDbUser;

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
    iamDbUser = envVars.get("CLOUD_SQL_IAM_USER").orElse(null);
  }

  protected void logParams() {
    log.info("Workbench SQL instance params: " + this.toString());
  }

  public boolean useIamAuth() {
    return iamDbUser != null;
  }

  public HikariConfig createConfig(String dbName) {
    HikariConfig config = new HikariConfig();
    config.setDriverClassName("com.mysql.cj.jdbc.Driver");
    config.setJdbcUrl(
        String.format("jdbc:mysql://%s/%s", useAppEngineSocket() ? "" : hostname, dbName));

    if (useIamAuth()) {
      config.setUsername(iamDbUser);
      // A non-empty password is required by the JDBC driver but ignored when using IAM auth.
      config.setPassword("ignored-but-required");
      config.addDataSourceProperty("enableIamAuth", "true");
      config.addDataSourceProperty("socketFactory", "com.google.cloud.sql.mysql.SocketFactory");
      config.addDataSourceProperty("cloudSqlInstance", cloudSqlInstanceName);
    } else {
      config.setUsername(username);
      config.setPassword(password);
      if (useAppEngineSocket()) {
        config.addDataSourceProperty("socketFactory", "com.google.cloud.sql.mysql.SocketFactory");
        config.addDataSourceProperty("cloudSqlInstance", cloudSqlInstanceName);
      }
    }
    return config;
  }

  public void validate() {
    if (hostname == null && cloudSqlInstanceName == null) {
      throw new IllegalStateException(
          "Database connection requires either a hostname (DB_HOST)"
              + " or a Cloud SQL instance name (CLOUD_SQL_INSTANCE_NAME).");
    }
    if (useIamAuth() && cloudSqlInstanceName == null) {
      throw new IllegalStateException(
          "CLOUD_SQL_IAM_USER requires CLOUD_SQL_INSTANCE_NAME to be set.");
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
    if (useIamAuth()) {
      return String.format(
          "[hostname:%s cloudSqlInstanceName:%s iamDbUser:%s authMode:IAM]",
          hostname, cloudSqlInstanceName, iamDbUser);
    }
    return String.format(
        "[hostname:%s cloudSqlInstanceName:%s username:%s password:%s]",
        hostname,
        cloudSqlInstanceName,
        username,
        password != null ? shadow(2, password) : null);
  }

  private static String shadow(int visibleCount, String s) {
    return s.substring(0, visibleCount) + s.substring(visibleCount).replaceAll(".", "*");
  }
}
