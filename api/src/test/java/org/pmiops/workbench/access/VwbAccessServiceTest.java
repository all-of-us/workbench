package org.pmiops.workbench.access;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.pmiops.workbench.config.WorkbenchConfig.createEmptyConfig;

import jakarta.inject.Provider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.vwb.sam.VwbSamClient;
import org.pmiops.workbench.vwb.usermanager.VwbUserManagerClient;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig
public class VwbAccessServiceTest {
  @MockBean private VwbSamClient mockVwbSamClient;
  @MockBean private VwbUserManagerClient mockVwbUserManagerClient;
  @MockBean private Provider<WorkbenchConfig> workbenchConfigProvider;
  private WorkbenchConfig workbenchConfig = createEmptyConfig();

  private VwbAccessService vwbAccessService;

  @BeforeEach
  public void setUp() {
    workbenchConfig.featureFlags.enableVWBUserAccessManagement = true;
    when(workbenchConfigProvider.get()).thenReturn(workbenchConfig);
    vwbAccessService =
        new VwbAccessService(mockVwbSamClient, mockVwbUserManagerClient, workbenchConfigProvider);
  }

  @Test
  public void testAddUserIntoVwbTier_vwbUm() {
    vwbAccessService.addUserIntoVwbTier("test-user", "test-group");
    verify(mockVwbUserManagerClient).addUserToGroup("test-user", "test-group");
    verify(mockVwbSamClient, never()).removeUserFromGroup(anyString(), anyString());
  }

  @Test
  public void testAddUserIntoVwbTier_fallOffSam() {
    doThrow(new RuntimeException("UM API failure"))
        .when(mockVwbUserManagerClient)
        .addUserToGroup(anyString(), anyString());
    vwbAccessService.addUserIntoVwbTier("test-user", "test-group");
    verify(mockVwbUserManagerClient).addUserToGroup("test-user", "test-group");
    verify(mockVwbSamClient).addUserToGroup("test-user", "test-group");
  }

  @Test
  public void testRemoveUserFromVwbTier_vwbUm() {
    vwbAccessService.removeUserFromVwbTier("test-user", "test-group");
    verify(mockVwbUserManagerClient).removeUserFromGroup("test-user", "test-group");
    verify(mockVwbSamClient, never()).removeUserFromGroup(anyString(), anyString());
  }

  @Test
  public void testRemoveUserFromVwbTier_fallOffSam() {
    doThrow(new RuntimeException("UM API failure"))
        .when(mockVwbUserManagerClient)
        .removeUserFromGroup(anyString(), anyString());
    vwbAccessService.addUserIntoVwbTier("test-user", "test-group");
    verify(mockVwbUserManagerClient).removeUserFromGroup("test-user", "test-group");
    verify(mockVwbSamClient).removeUserFromGroup("test-user", "test-group");
  }

  @Test
  public void testRemoveUserFromVwbTier_featureDisabled() {
    workbenchConfig.featureFlags.enableVWBUserAccessManagement = false;
    when(workbenchConfigProvider.get()).thenReturn(workbenchConfig);
    vwbAccessService.removeUserFromVwbTier("test-user", "test-group");
    verify(mockVwbUserManagerClient, never()).removeUserFromGroup(anyString(), anyString());
    verify(mockVwbSamClient, never()).removeUserFromGroup(anyString(), anyString());
  }
}
