package org.pmiops.workbench.google;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;

import java.time.Clock;
import java.util.Map;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.BaseIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.retry.backoff.ExponentialRandomBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

public class DirectoryServiceImplIntegrationTest extends BaseIntegrationTest {
  @Autowired private DirectoryService service;

  @TestConfiguration
  @ComponentScan(basePackageClasses = DirectoryServiceImpl.class)
  @Import({CloudStorageClientImpl.class, BaseIntegrationTest.Configuration.class})
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
  @Disabled("RW-7743")
  public void testCreateAndDeleteTestUser() {
    String userPrefix = String.format("integration.test.%d", Clock.systemUTC().millis());
    String username = userPrefix + "@" + config.googleDirectoryService.gSuiteDomain;
    service.createUser("Integration", "Test", username, "notasecret@gmail.com");
    boolean userNameTaken = retryTemplate().execute(c -> service.isUsernameTaken(userPrefix));
    assertThat(userNameTaken).isTrue();

    // As of ~6/25/19, customSchemas are sometimes unavailable on the initial call to Gsuite. This
    // data is likely not written with strong consistency. Retry until it is available.
    Map<String, Object> aouMeta =
        retryTemplate()
            .execute(
                c -> {
                  Map<String, Map<String, Object>> schemas =
                      service.getUserOrThrow(username).getCustomSchemas();
                  if (schemas == null) {
                    throw new RuntimeException("custom schemas is still null");
                  }
                  return schemas.get("All_of_Us_Workbench");
                });
    // Ensure our two custom schema fields are correctly set & re-fetched from GSuite.
    assertThat(aouMeta).containsEntry("Institution", "All of Us Research Workbench");
    assertThat(aouMeta)
        .containsEntry("Absorb_department_external_ID", config.absorb.externalDepartmentId);
    assertThat(service.getContactEmail(username)).hasValue("notasecret@gmail.com");

    retryTemplate()
        .execute(
            c -> {
              service.deleteUser(username);
              return null;
            });
    assertThat(service.isUsernameTaken(userPrefix)).isFalse();
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
