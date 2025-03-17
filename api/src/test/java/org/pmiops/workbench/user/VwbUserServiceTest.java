package org.pmiops.workbench.user;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import jakarta.inject.Provider;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbVwbUserPod;
import org.pmiops.workbench.vwb.user.ApiException;
import org.pmiops.workbench.vwb.user.model.OrganizationMember;
import org.pmiops.workbench.vwb.user.model.PodDescription;
import org.pmiops.workbench.vwb.user.model.PodRole;
import org.pmiops.workbench.vwb.user.model.UserDescription;
import org.pmiops.workbench.vwb.usermanager.VwbUserManagerClient;

@ExtendWith(MockitoExtension.class)
class VwbUserServiceTest {

  @Mock private VwbUserManagerClient vwbUserManagerClient;

  @Mock private Provider<WorkbenchConfig> workbenchConfigProvider;

  @Mock private UserDao userDao;

  @InjectMocks private VwbUserService vwbUserService;

  @BeforeEach
  public void setUp() {
    reset(workbenchConfigProvider);
  }

  void stub(boolean enableVWBUserAndPodCreation) {
    WorkbenchConfig workbenchConfig = WorkbenchConfig.createEmptyConfig();
    workbenchConfig.featureFlags.enableVWBUserAndPodCreation = enableVWBUserAndPodCreation;

    when(workbenchConfigProvider.get()).thenReturn(workbenchConfig);
  }

  @Test
  void createUser_featureFlagDisabled() {
    stub(false);

    vwbUserService.createUser("test@example.com");

    verify(vwbUserManagerClient, never()).getOrganizationMember(anyString());
  }

  @Test
  void createUser_userAlreadyExists() {
    stub(true);
    OrganizationMember organizationMember = mock(OrganizationMember.class);
    when(vwbUserManagerClient.getOrganizationMember("test@example.com"))
        .thenReturn(organizationMember);
    when(organizationMember.getUserDescription()).thenReturn(new UserDescription());

    vwbUserService.createUser("test@example.com");

    verify(vwbUserManagerClient, never()).createUser(anyString());
  }

  @Test
  void createUser_userDoesNotExist() {
    stub(true);

    OrganizationMember organizationMember = mock(OrganizationMember.class);
    when(vwbUserManagerClient.getOrganizationMember("test@example.com"))
        .thenReturn(organizationMember);
    when(organizationMember.getUserDescription()).thenReturn(null);

    vwbUserService.createUser("test@example.com");

    verify(vwbUserManagerClient).createUser("test@example.com");
  }

  @Test
  void createInitialCreditsPodForUser_featureFlagDisabled() {
    stub(false);

    DbUser dbUser = mock(DbUser.class);

    DbVwbUserPod result = vwbUserService.createInitialCreditsPodForUser(dbUser);

    assertNull(result);
    verify(vwbUserManagerClient, never()).createInitialCreditsPodForUser(anyString());
  }

  @Test
  void createInitialCreditsPodForUser_success() throws ApiException {
    stub(true);
    UUID uuid = UUID.randomUUID();

    DbUser dbUser = mock(DbUser.class);
    when(dbUser.getUsername()).thenReturn("test@example.com");
    PodDescription podDescription = mock(PodDescription.class);
    when(podDescription.getPodId()).thenReturn(uuid);
    when(vwbUserManagerClient.createInitialCreditsPodForUser("test@example.com"))
        .thenReturn(podDescription);

    DbVwbUserPod result = vwbUserService.createInitialCreditsPodForUser(dbUser);

    assertEquals(uuid.toString(), result.getVwbPodId());
    verify(vwbUserManagerClient).sharePodWithUserWithRole(uuid, "test@example.com", PodRole.ADMIN);
  }
}
