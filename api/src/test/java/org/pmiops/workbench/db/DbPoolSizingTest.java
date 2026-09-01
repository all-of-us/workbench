package org.pmiops.workbench.db;

import static com.google.common.truth.Truth.assertThat;

import com.zaxxer.hikari.HikariConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.cdr.DbParams;
import org.pmiops.workbench.config.EnvVars;

/**
 * Pool sizing is what keeps total connection usage proportional to load rather than to app instance
 * count. Before this was set, every pool took Hikari's default of minimumIdle == maximumPoolSize,
 * so each of the (CDR versions + 1) pools on an instance pinned 10 connections open forever and the
 * MySQL instance hit max_connections during scale-out.
 */
public class DbPoolSizingTest {

  /** EnvVars is a thin, overridable wrapper over System.getenv precisely so tests can do this. */
  private static class FakeEnvVars extends EnvVars {
    private final Map<String, String> values = new HashMap<>();

    FakeEnvVars set(String name, String value) {
      values.put(name, value);
      return this;
    }

    @Override
    public Optional<String> get(String name) {
      return Optional.ofNullable(values.get(name)).filter(s -> !s.isBlank());
    }
  }

  /**
   * Enough environment to get through Params' constructor without opening a socket: naming a Cloud
   * SQL instance rather than a host skips the connectivity probe in validate().
   */
  private static FakeEnvVars baseEnv() {
    return new FakeEnvVars()
        .set("CLOUD_SQL_INSTANCE_NAME", "project:region:workbench")
        .set("CDR_CLOUD_SQL_INSTANCE_NAME", "project:region:workbench")
        .set("WORKBENCH_DB_PASSWORD", "password")
        .set("CDR_DB_PASSWORD", "password")
        .set("GOOGLE_APPLICATION_CREDENTIALS", "/dev/null");
  }

  @Test
  public void primaryPoolKeepsAnIdleFloor() {
    HikariConfig config = new Params(baseEnv()).createConfig("workbench");

    // The primary pool serves user-facing requests, so it does not drain to zero: reconnecting
    // costs TCP, TLS, MySQL auth and a Cloud SQL token exchange on the request that pays it.
    assertThat(config.getMinimumIdle()).isEqualTo(2);
    assertThat(config.getMaximumPoolSize()).isEqualTo(10);
    assertThat(config.getIdleTimeout()).isEqualTo(300_000);
    assertThat(config.getMaxLifetime()).isEqualTo(1_500_000);
    assertThat(config.getKeepaliveTime()).isEqualTo(120_000);
  }

  @Test
  public void cdrPoolsDrainToZero() {
    HikariConfig config = new DbParams(baseEnv()).createConfig("c_2024q3_12");

    // There is one of these per CDR version per instance and all but the current one are idle,
    // so they hold nothing when unused. This is the difference between an instance pinning
    // (CDR versions + 1) * 10 connections and holding only what it is actively querying.
    assertThat(config.getMinimumIdle()).isEqualTo(0);
    assertThat(config.getMaximumPoolSize()).isEqualTo(10);
    assertThat(config.getIdleTimeout()).isEqualTo(300_000);
    assertThat(config.getMaxLifetime()).isEqualTo(1_500_000);
  }

  @Test
  public void maxLifetimeStaysUnderMysqlWaitTimeout() {
    // MySQL closes connections idle past wait_timeout; a pool that hands out a connection the
    // server already dropped surfaces a communications failure to the caller. Hikari must retire
    // connections first, so this must stay below the smallest wait_timeout of any environment.
    assertThat(new Params(baseEnv()).createConfig("workbench").getMaxLifetime())
        .isLessThan(1_800_000L);
  }

  @Test
  public void poolSizesAreEnvironmentTunable() {
    HikariConfig cdrConfig =
        new DbParams(baseEnv().set("CDR_DB_MAX_POOL_SIZE", "4").set("CDR_DB_MIN_IDLE", "1"))
            .createConfig("c_2024q3_12");
    assertThat(cdrConfig.getMaximumPoolSize()).isEqualTo(4);
    assertThat(cdrConfig.getMinimumIdle()).isEqualTo(1);

    HikariConfig primaryConfig =
        new Params(baseEnv().set("WORKBENCH_DB_MAX_POOL_SIZE", "20")).createConfig("workbench");
    assertThat(primaryConfig.getMaximumPoolSize()).isEqualTo(20);
  }

  @Test
  public void unparseableOverrideFallsBackToDefault() {
    // A typo in an environment variable must not take a pool to zero capacity.
    HikariConfig config =
        new DbParams(baseEnv().set("CDR_DB_MAX_POOL_SIZE", "not-a-number"))
            .createConfig("c_2024q3_12");
    assertThat(config.getMaximumPoolSize()).isEqualTo(10);
  }

  @Test
  public void poolsAreNamedPerDatabase() {
    // Pool names are how per-CDR usage is attributed in Hikari metrics, which is what
    // maximumPoolSize should be tuned from.
    assertThat(new DbParams(baseEnv()).createConfig("r_2019q4_10").getPoolName())
        .isEqualTo("workbench-r_2019q4_10");
  }
}
