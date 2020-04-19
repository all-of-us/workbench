package org.pmiops.workbench;

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import org.junit.Test;
import org.pmiops.workbench.firecloud.ApiClient;
import org.pmiops.workbench.firecloud.ApiException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.FireCloudServiceImpl;
import org.pmiops.workbench.firecloud.api.NihApi;
import org.pmiops.workbench.firecloud.api.ProfileApi;
import org.pmiops.workbench.firecloud.model.FirecloudMe;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

public class FireCloudIntegrationTest extends BaseIntegrationTest {

  @Autowired private FireCloudService service;

  @TestConfiguration
  @ComponentScan(basePackageClasses = FireCloudServiceImpl.class)
  @Import({FireCloudServiceImpl.class})
  static class Configuration {}

  @Test
  public void testStatusProd() throws IOException {
    config = loadProdConfig();
    assertThat(service.getApiBasePath()).isEqualTo("https://api.firecloud.org");
    assertThat(service.getFirecloudStatus()).isTrue();
  }

  @Test
  public void testStatusDev() {
    assertThat(service.getApiBasePath())
        .isEqualTo("https://firecloud-orchestration.dsde-dev.broadinstitute.org");
    assertThat(service.getFirecloudStatus()).isTrue();
  }

  /**
   * Ensures we can successfully use delegation of authority to make FireCloud API calls on behalf
   * of AoU users.
   *
   * <p>This test depends on there being an active account in FireCloud dev with the email address
   * integration-test-user@fake-research-aou.org.
   */
  @Test
  public void testImpersonatedProfileCall() throws Exception {
    ApiClient apiClient =
        service.getApiClientWithImpersonation("integration-test-user@fake-research-aou.org");

    // Run the most basic API call against the /me/ endpoint.
    ProfileApi profileApi = new ProfileApi(apiClient);
    FirecloudMe me = profileApi.me();
    assertThat(me.getUserInfo().getUserEmail())
        .isEqualTo("integration-test-user@fake-research-aou.org");
    assertThat(me.getUserInfo().getUserSubjectId()).isEqualTo("101727030557929965916");

    // Run a test against a different FireCloud endpoint. This is important, because the /me/
    // endpoint is accessible even by service accounts whose subject IDs haven't been whitelisted
    // by FireCloud devops.
    //
    // If we haven't had our "firecloud-admin" service account whitelisted,
    // then the following API call would result in a 401 error instead of a 404.
    NihApi nihApi = new NihApi(apiClient);
    int responseCode = 0;
    try {
      nihApi.nihStatus();
    } catch (ApiException e) {
      responseCode = e.getCode();
    }
    assertThat(responseCode).isEqualTo(404);
  }
}
