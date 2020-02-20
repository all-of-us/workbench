package org.pmiops.workbench.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
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

/**
 * A base class for functionality shared by many controller-level tests in the Workbench.
 *
 * <p>This class currently provides a simple way for test cases to use and modify the
 * WorkbenchConfig, for testing differential controller behavior depending on the environment
 * configuration.
 *
 * <p>Most or all controller tests should eventually extend this class, but adoption may proceed
 * incrementally to avoid a large one-time refactoring cost.
 *
 * <p>This class is heavily inspired by the BaseIntegrationTest class; maybe there is some way for
 * these two classes to share a common core, since they both care mostly about WorkbenchConfig bean
 * management for tests.
 *
 * <p>TODO(RW-4443): update all controller tests to extend this class.
 */
@RunWith(SpringRunner.class)
@DataJpaTest
public abstract class BaseControllerTest {

  protected static WorkbenchConfig config;
  public static final String BASE_PATH = "config/";

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
    // For some reason Resources.getResource(filename) works when running test from cmd line but
    // fails to find the resource when running from Intellij. This is a work around that finds the
    // config using a file path. This works for both cases.
    ObjectMapper jackson = new ObjectMapper();
    String rawJson =
        new String(Files.readAllBytes(Paths.get(BASE_PATH + filename)), Charset.defaultCharset());
    WorkbenchConfig workbenchConfig =
        new Gson().fromJson(jackson.readTree(rawJson).toString(), WorkbenchConfig.class);
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
