package org.pmiops.workbench;

import com.google.common.io.Resources;
import com.google.gson.Gson;
import java.io.IOException;
import java.nio.charset.Charset;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;

@RunWith(SpringRunner.class)
@WebAppConfiguration
@Import({IntegrationTestConfig.class})
@TestPropertySource(properties = "spring.main.allow-bean-definition-overriding=true")
public abstract class BaseIntegrationTest {

  protected static WorkbenchConfig config;

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
