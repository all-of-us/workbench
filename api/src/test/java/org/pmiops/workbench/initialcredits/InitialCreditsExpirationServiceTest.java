package org.pmiops.workbench.initialcredits;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;

import java.sql.Timestamp;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserInitialCreditsExpiration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({
  FakeClockConfiguration.class,
  InitialCreditsExpirationServiceImpl.class,
})
public class InitialCreditsExpirationServiceTest {

  @Autowired private InitialCreditsExpirationService service;

  @Test
  public void test_none() {
    DbUser user = new DbUser();
    assertThat(service.getCreditsExpiration(user)).isEmpty();
  }

  @Test
  public void test_userBypassed() {
    DbUser user =
        new DbUser()
            .setUserInitialCreditsExpiration(
                new DbUserInitialCreditsExpiration()
                    .setBypassed(true)
                    .setExpirationTime(new Timestamp(1234567890L)));
    assertThat(service.getCreditsExpiration(user)).isEmpty();
  }

  // TODO RW-13502
  //  @Test
  //  public void test_institutionBypassed() {
  //  }

  @Test
  public void test_nullTimestamp() {
    DbUser user =
        new DbUser()
            .setUserInitialCreditsExpiration(
                new DbUserInitialCreditsExpiration().setBypassed(false).setExpirationTime(null));
    assertThat(service.getCreditsExpiration(user)).isEmpty();
  }

  @Test
  public void test_validTimestamp() {
    Timestamp testTime = new Timestamp(1234567890L);
    DbUser user =
        new DbUser()
            .setUserInitialCreditsExpiration(
                new DbUserInitialCreditsExpiration()
                    .setBypassed(false)
                    .setExpirationTime(testTime));
    assertThat(service.getCreditsExpiration(user)).hasValue(testTime);
  }
}
