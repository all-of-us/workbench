package org.pmiops.workbench.captcha;

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.pmiops.workbench.api.BaseControllerTest;
import org.pmiops.workbench.captcha.api.CaptchaApi;
import org.pmiops.workbench.captcha.model.CaptchaVerificationResponse;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.google.CloudStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

public class CaptchaVerificationServiceTest extends BaseControllerTest {

  final String prodAllOfUsUrl = "https://workbench.researchallofus.org/login";
  final String testAllOfUsUrl = "https://all-of-us-workbench-test.appspot.com";
  final String responseToken = "responseToken";

  @MockBean private CloudStorageService cloudStorageService;
  @MockBean private WorkbenchConfig configProvider;
  @MockBean private CaptchaApi captchaApiProvider;

  @MockBean private WorkbenchConfig.AdminConfig adminConfig;
  @Autowired private CaptchaVerificationServiceImpl captchaVerificationService;

  @TestConfiguration
  @Import({CaptchaVerificationServiceImpl.class, CaptchaApi.class})
  static class Configuration {}

  @Before
  @Override
  public void setUp() throws IOException {
    super.setUp();
    Mockito.when(cloudStorageService.getCaptchaServerKey()).thenReturn("key");
    mockCaptchaResponse("hostName", true);
    captchaVerificationService.mockUseTestCaptcha(true);

  }

  private void mockCaptchaResponse(String hostName, boolean success) {
    CaptchaVerificationResponse response = new CaptchaVerificationResponse();
    response.setHostname(hostName);
    response.setSuccess(true);
    try {
      Mockito.when(captchaApiProvider.verify("key", responseToken)).thenReturn(response);
    } catch (ApiException ex) {

    }
  }

  @Test
  public void testCreateAccount_invalidHostName() {
    try {
      // This should return false since the host name can either be google test hostname or one of
      // AllOfUs urls
      captchaVerificationService.mockLoginUrl("hostname");
      captchaVerificationService.mockUseTestCaptcha(false);
      boolean captchaSuccess = captchaVerificationService.verifyCaptcha(responseToken);
      assertThat(captchaSuccess).isFalse();
    } catch (ApiException e) {
      e.printStackTrace();
      assertThat(false);
    }
  }

  /**
   * For any of the AllOfUs urls not using captcha test keys, the hostName should match exactly with
   * that send by google Captcha server
   */
  @Test
  public void testCreateAccount_nonCaptchaTestHosts() {
    try {
      mockCaptchaResponse("workbench.researchallofus.org", true);
      captchaVerificationService.mockLoginUrl(prodAllOfUsUrl);
      captchaVerificationService.mockUseTestCaptcha(false);

      boolean captchaSuccess = captchaVerificationService.verifyCaptcha(responseToken);
      assertThat(captchaSuccess).isTrue();
    } catch (ApiException e) {
      e.printStackTrace();
      assertThat(false);
    }
  }

  @Test
  public void testCreateAccount_googleTestKey() {
    try {
      // AllOfUs prod url should not be using google test captcha keys
      mockCaptchaResponse("testkey.google.com", true);
      captchaVerificationService.mockLoginUrl(prodAllOfUsUrl);
      boolean captchaSuccess = captchaVerificationService.verifyCaptcha(responseToken);
      assertThat(captchaSuccess).isFalse();

      // AllOfUs test url should be using google test captcha keys
      captchaVerificationService.mockLoginUrl(testAllOfUsUrl);
      captchaSuccess = captchaVerificationService.verifyCaptcha(responseToken);
      assertThat(captchaSuccess).isTrue();
    } catch (ApiException e) {
      e.printStackTrace();
      assertThat(false);
    }
  }
}
