package org.pmiops.workbench;

import com.google.common.io.Resources;
import com.google.gson.Gson;
import java.io.IOException;
import java.nio.charset.Charset;
import org.junit.runner.RunWith;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;

@RunWith(SpringRunner.class)
@WebAppConfiguration
@Import({IntegrationTestConfig.class})
public abstract class BaseIntegrationTest {

  static WorkbenchConfig config;

  @TestConfiguration
  static class Configuration {
    // This prototype-scoped bean override will cause all autowired services to call
    // this method for their Provider<WorkbenchConfig>. This allows test classes to either leave
    // this alone (in which case the test WorkbenchConfig is used), or make config modifications
    // before a test case which will be ready by all services.
    @Bean
    @Primary
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    WorkbenchConfig getTestConfig() throws IOException {
      if (config == null) {
        config = loadConfig("config_test.json");
      }
      return config;
    }
  }

  static WorkbenchConfig loadConfig(String filename) throws IOException {
    String testConfig =
        Resources.toString(Resources.getResource(filename), Charset.defaultCharset());
    WorkbenchConfig workbenchConfig = new Gson().fromJson(testConfig, WorkbenchConfig.class);
    workbenchConfig.firecloud.debugEndpoints = true;
    return workbenchConfig;
  }
}
