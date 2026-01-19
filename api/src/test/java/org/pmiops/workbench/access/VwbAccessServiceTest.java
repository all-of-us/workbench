package org.pmiops.workbench.access;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.pmiops.workbench.config.WorkbenchConfig.createEmptyConfig;

import jakarta.inject.Provider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbVwbUserPod;
import org.pmiops.workbench.user.VwbUserService;
import org.pmiops.workbench.vwb.usermanager.VwbUserManagerClient;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig
public class VwbAccessServiceTest {
  @MockBean private VwbUserManagerClient mockVwbUserManagerClient;
  @MockBean private Provider<WorkbenchConfig> workbenchConfigProvider;
  @MockBean private VwbUserService vwbUserService;
  private WorkbenchConfig workbenchConfig = createEmptyConfig();

  private VwbAccessService vwbAccessService;

  @BeforeEach
  public void setUp() {
    workbenchConfig.featureFlags.enableVWBUserAccessManagement = true;
    when(workbenchConfigProvider.get()).thenReturn(workbenchConfig);
    vwbAccessService = new VwbAccessService(mockVwbUserManagerClient, workbenchConfigProvider);
  }

  @Test
  public void testAddUserIntoVwbTier_vwbUm() {
    DbUser userWithPod = new DbUser().setUsername("test-user").setVwbUserPod(new DbVwbUserPod());
    when(vwbUserService.doesUserExist("test-user")).thenReturn(true);
    vwbAccessService.addUserIntoVwbTier(userWithPod, "test-group");
    verify(mockVwbUserManagerClient).addUserToGroup("test-group", "test-user");
  }

  @Test
  public void testRemoveUserFromVwbTier_vwbUm() {
    DbUser userWithPod = new DbUser().setUsername("test-user").setVwbUserPod(new DbVwbUserPod());
    vwbAccessService.removeUserFromVwbTier(userWithPod, "test-group");
    verify(mockVwbUserManagerClient).removeUserFromGroup("test-group", "test-user");
  }

  @Test
  public void testRemoveUserFromVwbTier_featureDisabled() {
    workbenchConfig.featureFlags.enableVWBUserAccessManagement = false;
    when(workbenchConfigProvider.get()).thenReturn(workbenchConfig);
    DbUser userWithPod = new DbUser().setUsername("test-user").setVwbUserPod(new DbVwbUserPod());
    vwbAccessService.removeUserFromVwbTier(userWithPod, "test-group");
    verify(mockVwbUserManagerClient, never()).removeUserFromGroup(anyString(), anyString());
  }
}
