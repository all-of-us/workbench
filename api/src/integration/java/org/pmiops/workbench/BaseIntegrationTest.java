package org.pmiops.workbench;

import com.google.common.io.Resources;
import com.google.gson.Gson;
import java.io.IOException;
import java.nio.charset.Charset;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;

@RunWith(SpringRunner.class)
@WebAppConfiguration
@Import({IntegrationTestConfig.class})
public abstract class BaseIntegrationTest {

  protected static WorkbenchConfig config;

  @TestConfiguration
  static class Configuration {
    // This prototype-scoped bean override will cause all autowired services to call
    // this method for their Provider<WorkbenchConfig>. Concrete tests should assign
    // a value to `config` from within setUp and null it out from within tearDown. Further
    // modifications may be made from within each test case.
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    WorkbenchConfig getTestConfig() throws IOException {
      return config;
    }
  }

  @Before
  public void setUp() throws IOException {
    config = loadTestConfig();
  }

  @After
  public void tearDown() {
    config = null;
  }

  private static WorkbenchConfig loadConfig(String filename) throws IOException {
    String testConfig =
        Resources.toString(Resources.getResource(filename), Charset.defaultCharset());
    WorkbenchConfig workbenchConfig = new Gson().fromJson(testConfig, WorkbenchConfig.class);
    workbenchConfig.firecloud.debugEndpoints = true;
    return workbenchConfig;
  }

  protected static WorkbenchConfig loadTestConfig() throws IOException {
    return loadConfig("config_test.json");
  }

  protected static WorkbenchConfig loadProdConfig() throws IOException {
    return loadConfig("config_prod.json");
  }
}
