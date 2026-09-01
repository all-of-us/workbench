package org.pmiops.workbench.cdr;

import com.zaxxer.hikari.HikariConfig;
import java.util.logging.Logger;
import org.pmiops.workbench.config.EnvVars;
import org.pmiops.workbench.db.Params;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DbParams extends Params {
  private static final Logger log = Logger.getLogger(DbParams.class.getName());

  public DbParams(EnvVars envVars) {
    super(envVars);
  }

  @Override
  public void loadFromEnvironment() {
    hostname = envVars.get("CDR_DB_HOST").orElse(null);
    cloudSqlInstanceName = envVars.get("CDR_CLOUD_SQL_INSTANCE_NAME").orElse(null);
    password = envVars.get("CDR_DB_PASSWORD").orElse(null);
    try {
      validate();
      log.info("CDR SQL instance params: " + this.toString());
    } catch (IllegalStateException e) {
      super.loadFromEnvironment();
      log.info("CDR SQL instance params: [Workbench instance params]");
    }
  }

  protected void logParams() {
    // Logged above.
  }

  /**
   * Pool sizing for the per-CDR-version pools built by {@link CdrDataSource}. There is one of these
   * per row in cdr_version, per app instance, and all but the current CDR are near-permanently idle
   * -- so unlike the primary workbench pool they get no idle floor. minimumIdle=0 lets idleTimeout
   * drain an unused CDR pool to zero connections instead of holding maximumPoolSize open for the
   * life of the instance.
   *
   * <p>The extra connection setup this costs on a cold pool is not on any hot path: it is paid once
   * per idleTimeout window by whoever next queries that CDR version.
   */
  @Override
  protected void applyPoolSizing(HikariConfig config) {
    config.setMaximumPoolSize(intFromEnv("CDR_DB_MAX_POOL_SIZE", DEFAULT_MAX_POOL_SIZE));
    config.setMinimumIdle(intFromEnv("CDR_DB_MIN_IDLE", 0));
    config.setIdleTimeout(IDLE_TIMEOUT_MS);
    config.setMaxLifetime(MAX_LIFETIME_MS);
  }
}
