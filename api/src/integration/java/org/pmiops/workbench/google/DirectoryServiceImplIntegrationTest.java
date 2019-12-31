package org.pmiops.workbench.google;

import static com.google.common.truth.Truth.assertThat;

import java.time.Clock;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.retry.backoff.ExponentialRandomBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class DirectoryServiceImplIntegrationTest {
  @Autowired private DirectoryService service;

  @TestConfiguration
  @Import({DirectoryServiceImpl.class})
  static class Configuration {}

  @Test
  public void testDummyUsernameIsNotTaken() {
    assertThat(service.isUsernameTaken("username-that-should-not-exist")).isFalse();
  }

  @Test
  public void testDirectoryServiceUsernameIsTaken() {
    assertThat(service.isUsernameTaken("directory-service")).isTrue();
  }

  @Test
  public void testCreateAndDeleteTestUser() {
    String userName = String.format("integration.test.%d", Clock.systemUTC().millis());
    service.createUser("Integration", "Test", userName, "notasecret@gmail.com");
    assertThat(service.isUsernameTaken(userName)).isTrue();

    // As of ~6/25/19, customSchemas are sometimes unavailable on the initial call to Gsuite. This
    // data is likely not written with strong consistency. Retry until it is available.
    Map<String, Object> aouMeta =
        retryTemplate()
            .execute(
                c -> {
                  Map<String, Map<String, Object>> schemas =
                      service.getUserByUsername(userName).getCustomSchemas();
                  if (schemas == null) {
                    throw new RuntimeException("custom schemas is still null");
                  }
                  return schemas.get("All_of_Us_Workbench");
                });
    // Ensure our two custom schema fields are correctly set & re-fetched from GSuite.
    assertThat(aouMeta).containsEntry("Institution", "All of Us Research Workbench");
    assertThat(service.getContactEmailAddress(userName)).isEqualTo("notasecret@gmail.com");
    service.deleteUser(userName);
    assertThat(service.isUsernameTaken(userName)).isFalse();
  }

  private static RetryTemplate retryTemplate() {
    RetryTemplate tmpl = new RetryTemplate();
    ExponentialRandomBackOffPolicy backoff = new ExponentialRandomBackOffPolicy();
    tmpl.setBackOffPolicy(backoff);
    SimpleRetryPolicy retry = new SimpleRetryPolicy();
    retry.setMaxAttempts(10);
    tmpl.setRetryPolicy(retry);
    tmpl.setThrowLastExceptionOnExhausted(true);
    return tmpl;
  }
}
