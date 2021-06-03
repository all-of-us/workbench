package org.pmiops.workbench.captcha;

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.pmiops.workbench.captcha.api.CaptchaApi;
import org.pmiops.workbench.captcha.model.CaptchaVerificationResponse;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.google.CloudStorageClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;

public class CaptchaVerificationServiceTest {

  final String prodAllOfUsUrl = "https://workbench.researchallofus.org/login";
  final String testUrl = "testkey.google.com";
  final String responseToken = "responseToken";

  @MockBean private CloudStorageClient cloudStorageClient;
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

  @BeforeEach
  public void setUp() throws IOException, ApiException {
    config = WorkbenchConfig.createEmptyConfig();
    config.admin.loginUrl = "hostname";
    Mockito.when(cloudStorageClient.getCaptchaServerKey()).thenReturn("key");
    mockCaptchaResponse("hostName", true);
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
    // AllOfUs urls
    config.captcha.useTestCaptcha = false;
    config.admin.loginUrl = "fakeHostName";
    boolean captchaSuccess = captchaVerificationService.verifyCaptcha(responseToken);
    assertThat(captchaSuccess).isFalse();
  }

  /**
   * For any of the AllOfUs urls not using captcha test keys, the hostName should match exactly with
   * that send by google Captcha server
   *
   * @throws ApiException
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
    mockCaptchaResponse(testUrl, true);
    config.captcha.useTestCaptcha = true;

    boolean captchaSuccess = captchaVerificationService.verifyCaptcha(responseToken);
    assertThat(captchaSuccess).isTrue();
  }
}
