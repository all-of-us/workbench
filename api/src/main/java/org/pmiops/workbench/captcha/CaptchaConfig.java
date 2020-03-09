package org.pmiops.workbench.captcha;

import org.pmiops.workbench.captcha.api.CaptchaApi;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.context.annotation.RequestScope;

@Configuration
public class CaptchaConfig {

  @Bean
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public org.pmiops.workbench.captcha.ApiClient captchaApiClient(WorkbenchConfig workbenchConfig) {
    return new org.pmiops.workbench.captcha.ApiClient();
  }

  @Bean
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public CaptchaApi captchaApi(ApiClient apiClient) {
    CaptchaApi api = new CaptchaApi();
    api.setApiClient(apiClient);
    return api;
  }
}
