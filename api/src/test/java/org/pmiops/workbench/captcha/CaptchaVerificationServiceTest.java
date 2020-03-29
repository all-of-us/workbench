package org.pmiops.workbench.captcha;

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.pmiops.workbench.captcha.api.CaptchaApi;
import org.pmiops.workbench.captcha.model.CaptchaVerificationResponse;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.google.CloudStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class CaptchaVerificationServiceTest {

  final String prodAllOfUsUrl = "https://workbench.researchallofus.org/login";
  final String testAllOfUsUrl = "https://all-of-us-workbench-test.appspot.com";
  final String responseToken = "responseToken";

  @MockBean private CloudStorageService cloudStorageService;
  @MockBean private CaptchaApi captchaApiProvider;

  @Autowired private CaptchaVerificationServiceImpl captchaVerificationService;

  private static WorkbenchConfig config;

  @TestConfiguration
  @Import({CaptchaVerificationServiceImpl.class, CaptchaApi.class})
  static class Configuration {
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public WorkbenchConfig workbenchConfig() {
      return config;
    }
  }

  @Before
  public void setUp() throws IOException {
    config = WorkbenchConfig.createEmptyConfig();
    config.admin.loginUrl = "hostname";
    Mockito.when(cloudStorageService.getCaptchaServerKey()).thenReturn("key");
    try {
      mockCaptchaResponse("hostName", true);
    } catch (ApiException ex) {
      ex.printStackTrace();
    }
  }

  private void mockCaptchaResponse(String hostName, boolean success) throws ApiException {
    CaptchaVerificationResponse response = new CaptchaVerificationResponse();
    response.setHostname(hostName);
    response.setSuccess(true);
    config.captcha.useTestCaptcha = true;
    Mockito.when(captchaApiProvider.verify("key", responseToken)).thenReturn(response);
  }

  @Test
  public void testCreateAccount_invalidHostName() throws ApiException {
    // This should return false since the host name can either be google test hostname or one of
    // AllOfUs urls
    config.captcha.useTestCaptcha = false;
    boolean captchaSuccess = captchaVerificationService.verifyCaptcha(responseToken);
    assertThat(captchaSuccess).isFalse();
  }

  /**
   * For any of the AllOfUs urls not using captcha test keys, the hostName should match exactly with
   * that send by google Captcha server
   */
  @Test
  public void testCreateAccount_nonCaptchaTestHosts() throws ApiException {
    mockCaptchaResponse("workbench.researchallofus.org", true);
    config.admin.loginUrl = prodAllOfUsUrl;
    config.captcha.useTestCaptcha = false;

    boolean captchaSuccess = captchaVerificationService.verifyCaptcha(responseToken);
    assertThat(captchaSuccess).isTrue();
  }

  @Test
  public void testCreateAccount_googleTestKey() throws ApiException {
    // AllOfUs prod url should not be using google test captcha keys
    mockCaptchaResponse("testkey.google.com", true);
    config.admin.loginUrl = prodAllOfUsUrl;
    config.captcha.useTestCaptcha = false;
    boolean captchaSuccess = captchaVerificationService.verifyCaptcha(responseToken);
    assertThat(captchaSuccess).isFalse();

    // AllOfUs test url should be using google test captcha keys
    config.admin.loginUrl = testAllOfUsUrl;
    config.captcha.useTestCaptcha = true;

    captchaSuccess = captchaVerificationService.verifyCaptcha(responseToken);
    assertThat(captchaSuccess).isTrue();
  }
}
