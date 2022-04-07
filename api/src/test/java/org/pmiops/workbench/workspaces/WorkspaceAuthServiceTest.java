package org.pmiops.workbench.workspaces;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.pmiops.workbench.access.AccessTierService;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.BillingStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig
public class WorkspaceAuthServiceTest {

  @Autowired private WorkspaceAuthService workspaceAuthService;

  @MockBean private WorkspaceDao mockWorkspaceDao;

  @TestConfiguration
  @Import(WorkspaceAuthService.class)
  @MockBean({
    AccessTierService.class,
    FireCloudService.class,
  })
  static class Configuration {}

  @Test
  public void test_validateActiveBilling_active() {
    final String namespace = "wsns";
    final String fcName = "firecloudname";
    stubRequired(namespace, fcName, BillingStatus.ACTIVE);

    // does not throw
    workspaceAuthService.validateActiveBilling(namespace, fcName);
  }

  @Test
  public void test_validateActiveBilling_inactive() {
    final String namespace = "wsns";
    final String fcName = "firecloudname";
    stubRequired(namespace, fcName, BillingStatus.INACTIVE);

    assertThrows(ForbiddenException.class, () -> workspaceAuthService.validateActiveBilling(namespace, fcName));
  }

  private void stubRequired(String namespace, String fcName, BillingStatus billingStatus) {
    final DbWorkspace toReturn =
        new DbWorkspace()
            .setWorkspaceNamespace(namespace)
            .setFirecloudName(fcName)
            .setBillingStatus(billingStatus);
    when(mockWorkspaceDao.getRequired(namespace, fcName)).thenReturn(toReturn);
  }
}
