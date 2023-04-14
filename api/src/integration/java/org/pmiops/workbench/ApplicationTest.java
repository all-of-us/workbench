package org.pmiops.workbench;

import static com.google.common.truth.Truth.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.cdr.CdrDataSource;
import org.pmiops.workbench.cdr.DbParams;
import org.pmiops.workbench.db.Params;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * This test that all application injection is done properly. It loads all JPA repositories,
 * Services, Controllers, Components and Configurations.
 */
@SpringBootTest
@AutoConfigureMockMvc
// @Import(ApplicationTest.Configuration.class)
public class ApplicationTest {

  @Autowired private ApplicationContext context;
@Autowired private MockMvc mockMvc;

  @MockBean(name = "params")
  private Params params;

  @MockBean private DataSource dataSource;
  @MockBean private DbParams cdrParams;
  @MockBean private CdrDataSource cdrDataSource;

  @TestConfiguration
  static class Configuration {
    @Bean
//     @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
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
    this.mockMvc.perform(get("/")).andDo(print()).andExpect(status().isOk())
          .andExpect(content().string("AllOfUs Workbench API"));
  }
}
