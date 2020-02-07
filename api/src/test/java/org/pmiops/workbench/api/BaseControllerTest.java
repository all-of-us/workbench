package org.pmiops.workbench.api;

import com.google.common.io.Resources;
import com.google.gson.Gson;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Random;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.test.FakeLongRandom;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
public class BaseControllerTest {

  protected static WorkbenchConfig config;

  @TestConfiguration
  static class Configuration {
    // This prototype-scoped bean override will cause all autowired services to call this method
    // for their Provider<WorkbenchConfig>. Further modifications may be made from within each test
    // case.
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    WorkbenchConfig getIntegrationTestConfig() throws IOException {
      return config;
    }

    @Bean
    Random random() {
      return new FakeLongRandom(123);
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
