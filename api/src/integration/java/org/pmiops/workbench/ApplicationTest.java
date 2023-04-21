package org.pmiops.workbench;

import static com.google.common.truth.Truth.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.google.cloud.iam.credentials.v1.IamCredentialsClient;
import com.google.cloud.monitoring.v3.MetricServiceClient;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import com.zaxxer.hikari.HikariConfig;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.pmiops.workbench.cdr.DbParams;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.Params;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.stereotype.Component;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * This test that all application injection is done properly. It loads all JPA repositories,
 * Services, Controllers, Components and Configurations.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.main.allow-bean-definition-overriding=true")
public class ApplicationTest {

  @Autowired private ApplicationContext context;
  @Autowired private MockMvc mockMvc;

  // IamCredentialsClient and MetricServiceClient require an App Engine environment or
  // GOOGLE_APPLICATION_CREDENTIALS to be defined. We can avoid this requirement by mocking.
  @MockBean private IamCredentialsClient iamCredentialsClient;
  @MockBean private MetricServiceClient metricServiceClient;

  @TestConfiguration
  static class Configuration {

    @Bean(name = "params")
    Params getParams() {
      Params params = Mockito.mock(Params.class);
      Mockito.when(params.createConfig("workbench")).thenReturn(createMemDbConfig());
      return params;
    }

    @Bean(name = "dbParams")
    DbParams getDbParams() {
      DbParams params = Mockito.mock(DbParams.class);
      Mockito.when(params.createConfig("workbench")).thenReturn(createMemDbConfig());
      return params;
    }

    private HikariConfig createMemDbConfig() {
      HikariConfig dbConfig = new HikariConfig();
      dbConfig.setDriverClassName("org.h2.Driver");
      dbConfig.setJdbcUrl("jdbc:h2:mem:db;DB_CLOSE_DELAY=-1");
      dbConfig.setUsername("sa");
      dbConfig.setPassword("sa");
      return dbConfig;
    }

    @Bean
    @Primary
    WorkbenchConfig getApplicationTestWorkbenchConfig() throws IOException {
      WorkbenchConfig c = loadConfig();
      return c;
    }

    private static WorkbenchConfig loadConfig() throws IOException {
      String testConfig =
          Resources.toString(Resources.getResource("config_test.json"), Charset.defaultCharset());
      WorkbenchConfig workbenchConfig = new Gson().fromJson(testConfig, WorkbenchConfig.class);
      workbenchConfig.firecloud.debugEndpoints = true;
      return workbenchConfig;
    }
  }

  @Test
  public void contextLoads() {
    List<Object> beans = new ArrayList<>();
    // This loads all dao's that implement JPA repositories
    beans.addAll(context.getBeansWithAnnotation(NoRepositoryBean.class).values());
    // This loads all @Service, @Controller, @Component and @Configuration annotations
    beans.addAll(context.getBeansWithAnnotation(Component.class).values());
    for (Object object : beans) {
      assertThat(object).isNotNull();
    }
  }

  @Test
  public void shouldReturnDefaultMessage() throws Exception {
    this.mockMvc
        .perform(get("/"))
        //.andDo(print())
        .andExpect(status().isOk())
        .andExpect(content().string("AllOfUs Workbench API"));
  }
}
