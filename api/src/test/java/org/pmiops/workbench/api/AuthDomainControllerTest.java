package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudManagedGroupWithMembers;
import org.pmiops.workbench.model.AuthDomainCreatedResponse;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Import(FakeClockConfiguration.class)
public class AuthDomainControllerTest {
  @Mock private FireCloudService fireCloudService;

  private AuthDomainController authDomainController;

  @BeforeEach
  public void setUp() {
    this.authDomainController = new AuthDomainController(fireCloudService);
  }

  @Test
  public void testCreateAuthDomain() {
    final String testDomain = "my-auth-domain";
    final String testGroupEmail = "test-group@google.com";

    when(fireCloudService.createGroup(any()))
        .thenReturn(new FirecloudManagedGroupWithMembers().groupEmail(testGroupEmail));

    final ResponseEntity<AuthDomainCreatedResponse> response =
        authDomainController.createAuthDomain(testDomain);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody())
        .isEqualTo(
            new AuthDomainCreatedResponse().authDomainName(testDomain).groupEmail(testGroupEmail));
  }
}
