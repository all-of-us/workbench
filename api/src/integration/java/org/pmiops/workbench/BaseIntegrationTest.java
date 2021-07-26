package org.pmiops.workbench;

import com.google.common.io.Resources;
import com.google.gson.Gson;
import java.io.IOException;
import java.nio.charset.Charset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;

@WebAppConfiguration
@Import({IntegrationTestConfig.class})
@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = "spring.main.allow-bean-definition-overriding=true")
public abstract class BaseIntegrationTest {

  protected static WorkbenchConfig config;

  @TestConfiguration
  public static class Configuration {
    // This prototype-scoped bean override will cause all autowired services to call this method
    // for their Provider<WorkbenchConfig>. Further modifications may be made from within each test
    // case.
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    WorkbenchConfig getIntegrationTestConfig() throws IOException {
      return config;
    }
  }

  @BeforeEach
  public void setUp() throws IOException {
    config = loadTestConfig();
  }

  @AfterEach
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
