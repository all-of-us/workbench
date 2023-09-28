package org.pmiops.workbench;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.Test;
import org.pmiops.workbench.absorb.api.AuthenticateApi;
import org.pmiops.workbench.absorb.api.EnrollmentsApi;
import org.pmiops.workbench.absorb.api.UsersApi;
import org.pmiops.workbench.absorb.model.AuthenticationRequest;
import org.pmiops.workbench.google.CloudStorageClient;
import org.pmiops.workbench.google.DirectoryServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;

public class AbsorbAcceptanceTest extends BaseIntegrationTest {
  @Autowired private CloudStorageClient cloudStorageClient;

  @TestConfiguration
  @ComponentScan(basePackageClasses = DirectoryServiceImpl.class)
  static class Configuration {}

  @Test
  public void testGetCertificates() throws Exception {
    // Setup: A user with this email exists and has completed both Absorb trainings in the
    // test environment.
    var email = "absorb_acceptance_test@fake-research-aou.org";

    var config = cloudStorageClient.getAbsorbCredentials();
    var apiKey = config.getString("apiKey");

    // Obtain an access token
    var accessToken =
        new AuthenticateApi()
            .restAuthenticationAuthenticate(
                new AuthenticationRequest()
                    .password(config.getString("password"))
                    .username(config.getString("username"))
                    .privateKey(apiKey),
                apiKey);

    // Find the user id
    var users = new UsersApi().usersGetUsers(apiKey, accessToken, "username eq '" + email + "'");
    assertThat(users.getUsers().size()).isEqualTo(1);
    assertThat(users.getUsers().get(0).getUsername()).isEqualTo(email);

    // Validate the user has completed two courses
    var enrollments =
        new EnrollmentsApi()
            .enrollmentsGetUserEnrollments(
                apiKey, accessToken, users.getUsers().get(0).getId(), null);
    assertThat(enrollments.getEnrollments().size()).isEqualTo(2);
    for (var enrollment : enrollments.getEnrollments()) {
      assertThat(enrollment.getDateCompleted()).isNotNull();
    }
  }
}
