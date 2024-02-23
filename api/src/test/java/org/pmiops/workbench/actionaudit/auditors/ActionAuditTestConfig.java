package org.pmiops.workbench.actionaudit.auditors;

import static org.pmiops.workbench.actionaudit.ActionAuditSpringConfiguration.ACTION_ID_BEAN;

import java.time.Clock;
import java.time.Instant;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.test.FakeClock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Common beans used by action audit test classes. */
@Configuration
public class ActionAuditTestConfig {

  public static final Instant INSTANT = Instant.parse("2000-01-01T00:00:00.00Z");
  public static final String ACTION_ID = "9095d2f9-8db2-46c3-8f8e-4f90a62b457f";
  public static final long ADMINISTRATOR_USER_ID = 222L;
  public static final String ADMINISTRATOR_EMAIL = "admin@aou.biz";

  @Bean
  public static DbUser getUser() {
    final DbUser administrator = new DbUser();
    administrator.setUserId(ADMINISTRATOR_USER_ID);
    administrator.setUsername(ADMINISTRATOR_EMAIL);
    return administrator;
  }

  @Bean(name = ACTION_ID_BEAN)
  public String getActionId() {
    return ACTION_ID;
  }

  @Bean
  public Clock getClock() {
    return new FakeClock(INSTANT);
  }
}
