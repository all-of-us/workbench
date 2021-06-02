package org.pmiops.workbench.shibboleth;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.config.RetryConfig;
import org.pmiops.workbench.shibboleth.api.ShibbolethApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;


public class ShibbolethServiceImplTest {

  @Autowired private ShibbolethService shibbolethService;

  @MockBean private ShibbolethApi shibbolethApi;

  @TestConfiguration
  @Import({ShibbolethServiceImpl.class, RetryConfig.class})
  static class Configuration {}

  @Test
  public void testUpdateShibbolethToken() throws Exception {
    shibbolethService.updateShibbolethToken("asdf");
    verify(shibbolethApi, times(1)).postShibbolethToken("asdf");
  }
}
