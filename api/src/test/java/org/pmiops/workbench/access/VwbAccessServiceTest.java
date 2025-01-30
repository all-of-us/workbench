package org.pmiops.workbench.access;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.pmiops.workbench.config.WorkbenchConfig.createEmptyConfig;

import jakarta.inject.Provider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.vwb.sam.VwbSamClient;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig
public class VwbAccessServiceTest {
  @MockBean private VwbSamClient mockVwbSamClient;
  @MockBean private Provider<WorkbenchConfig> workbenchConfigProvider;
  @Mock private WorkbenchConfig workbenchConfig;

  private VwbAccessService vwbAccessService;

  @BeforeEach
  public void setUp() {
    WorkbenchConfig workbenchConfig = createEmptyConfig();
    workbenchConfig.featureFlags.enableVWBUserAccessManagement = true;
    when(workbenchConfigProvider.get()).thenReturn(workbenchConfig);
    vwbAccessService = new VwbAccessService(mockVwbSamClient, workbenchConfigProvider);
  }

  @Test
  public void testAddUserIntoVwbTier_featureEnabled() {
    vwbAccessService.addUserIntoVwbTier("test-user", "test-group");
    verify(mockVwbSamClient).addUserToGroup("test-user", "test-group");
  }

  @Test
  public void testAddUserIntoVwbTier_featureDisabled() {
    vwbAccessService.removeUserFromVwbTier("test-user", "test-group");
    verify(mockVwbSamClient).removeUserFromGroup("test-user", "test-group");
  }

  @Test
  public void testRemoveUserFromVwbTier_featureDisabled() {
    workbenchConfig.featureFlags.enableVWBUserAccessManagement = true;
    when(workbenchConfigProvider.get()).thenReturn(workbenchConfig);

    vwbAccessService.removeUserFromVwbTier("test-user", "test-group");

    verify(mockVwbSamClient, never()).removeUserFromGroup(anyString(), anyString());
  }
}
