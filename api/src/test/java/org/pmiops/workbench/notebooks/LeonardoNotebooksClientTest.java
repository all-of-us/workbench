package org.pmiops.workbench.notebooks;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.config.WorkbenchConfig.FireCloudConfig;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.leonardo.LeonardoRetryHandler;
import org.pmiops.workbench.leonardo.api.RuntimesApi;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.retry.backoff.NoBackOffPolicy;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class LeonardoNotebooksClientTest {
  private static final Instant NOW = Instant.parse("1988-12-26T00:00:00Z");
  private static final FakeClock CLOCK = new FakeClock(NOW, ZoneId.systemDefault());
  private static final Duration MAX_AGE = Duration.ofDays(14);
  private static final Duration IDLE_MAX_AGE = Duration.ofDays(7);

  @Qualifier(NotebooksConfig.USER_RUNTIMES_API)
  @MockBean
  private RuntimesApi mockRuntimesApi;

  @MockBean
  private WorkspaceService workspaceService;

  @Autowired private LeonardoNotebooksClient leonardoNotebooksClient;

  @TestConfiguration
  @Import({LeonardoNotebooksClientImpl.class, LeonardoRetryHandler.class, NoBackOffPolicy.class})
  @MockBean({
      NotebooksRetryHandler.class
  })
  static class Configuration {

    @Bean
    Clock clock() {
      return CLOCK;
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    DbUser getDbUser() {
      DbUser dbUser = new DbUser();
      dbUser.setUsername("test@aou.org");
      return dbUser;
    }

    @Bean
    public WorkbenchConfig workbenchConfig() {
      WorkbenchConfig config = WorkbenchConfig.createEmptyConfig();
      config.firecloud = new FireCloudConfig();
      config.firecloud.notebookRuntimeMaxAgeDays = (int) MAX_AGE.toDays();
      config.firecloud.notebookRuntimeIdleMaxAgeDays = (int) IDLE_MAX_AGE.toDays();
      return config;
    }
  }

  @Before
  public void setUp() {
    doReturn(new DbWorkspace()).when(workspaceService).getRequired(anyString(), anyString());
  }

  @Test
  public void createRuntime_defaultLabel() throws Exception {
    leonardoNotebooksClient.createRuntime("a", "b", "c");
    verify(mockRuntimesApi).createRuntime(any(), any(), any());
  }

  @Test
  public void createRuntime_overrideLabel() throws Exception {
    leonardoNotebooksClient.createRuntime("a", "b", "c");
    verify(mockRuntimesApi).createRuntime(any(), any(), any());
  }
}
