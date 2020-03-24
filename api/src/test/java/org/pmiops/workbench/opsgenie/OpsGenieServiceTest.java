package org.pmiops.workbench.opsgenie;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.verify;

import com.ifountain.opsgenie.client.swagger.ApiException;
import com.ifountain.opsgenie.client.swagger.api.AlertApi;
import com.ifountain.opsgenie.client.swagger.model.CreateAlertRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.model.EgressEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class OpsGenieServiceTest {
  private static WorkbenchConfig workbenchConfig;
  private EgressEvent egressEvent;

  @MockBean private AlertApi mockAlertApi;
  @Captor private ArgumentCaptor<CreateAlertRequest> alertRequestCaptor;
  @Autowired private OpsGenieService opsGenieService;

  @TestConfiguration
  @Import({OpsGenieServiceImpl.class})
  static class Configuration {
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    WorkbenchConfig getWorkbenchConfig() {
      return workbenchConfig;
    }
  }

  @Before
  public void setUp() {
    workbenchConfig = WorkbenchConfig.createEmptyConfig();
    workbenchConfig.server.clientBaseUrl = "https://workbench.researchallofus.org";

    egressEvent =
        new EgressEvent()
            .projectName("aou-rw-test-c7dec260")
            .vmName("aou-rw-1")
            .egressMib(120.7)
            .egressMibThreshold(100.0)
            .timeWindowDuration(600L);
  }

  @Test
  public void createEgressEventAlert() throws ApiException {
    opsGenieService.createEgressEventAlert(egressEvent);
    verify(mockAlertApi).createAlert(alertRequestCaptor.capture());

    CreateAlertRequest request = alertRequestCaptor.getValue();
    assertThat(request.getDescription()).contains("Workspace project: aou-rw-test-c7dec260");
    assertThat(request.getDescription())
        .contains("https://workbench.researchallofus.org/admin/workspaces/aou-rw-test-c7dec260/");
    assertThat(request.getAlias()).isEqualTo("aou-rw-test-c7dec260 - aou-rw-1");
  }
}
