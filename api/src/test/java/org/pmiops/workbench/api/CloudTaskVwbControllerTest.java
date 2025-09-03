package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import jakarta.inject.Provider;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbVwbUserPod;
import org.pmiops.workbench.initialcredits.InitialCreditsService;
import org.pmiops.workbench.model.CreateVwbPodTaskRequest;
import org.pmiops.workbench.user.VwbUserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class CloudTaskVwbControllerTest {

  @Mock private VwbUserService mockVwbUserService;
  @Mock private UserService mockUserService;
  @Mock private Provider<WorkbenchConfig> mockWorkbenchConfigProvider;
  @Mock private InitialCreditsService mockInitialCreditsService;
  @Mock private WorkbenchConfig mockWorkbenchConfig;
  @Mock private WorkbenchConfig.FeatureFlagsConfig mockFeatureFlags;

  private CloudTaskVwbController controller;
  private static final String TEST_USERNAME = "test-user@example.com";

  @BeforeEach
  void setUp() {
    controller =
        new CloudTaskVwbController(
            mockVwbUserService,
            mockUserService,
            mockWorkbenchConfigProvider,
            mockInitialCreditsService);

    when(mockWorkbenchConfigProvider.get()).thenReturn(mockWorkbenchConfig);
    mockWorkbenchConfig.featureFlags = mockFeatureFlags;
  }

  @Test
  void processCreateVwbPodTask_whenFeatureFlagDisabled_returnsBadRequest() {
    // Arrange
    mockFeatureFlags.enableVWBUserAndPodCreation = false;
    CreateVwbPodTaskRequest request = new CreateVwbPodTaskRequest();
    request.setUserName(TEST_USERNAME);

    // Act
    ResponseEntity<Void> response = controller.processCreateVwbPodTask(request);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    verifyNoInteractions(mockUserService, mockVwbUserService, mockInitialCreditsService);
  }

  @Test
  void processCreateVwbPodTask_whenUserNotFound_returnsNoContent() {
    // Arrange
    mockFeatureFlags.enableVWBUserAndPodCreation = true;
    CreateVwbPodTaskRequest request = new CreateVwbPodTaskRequest();
    request.setUserName(TEST_USERNAME);

    when(mockUserService.getByUsername(TEST_USERNAME)).thenReturn(Optional.empty());

    // Act
    ResponseEntity<Void> response = controller.processCreateVwbPodTask(request);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    verify(mockUserService).getByUsername(TEST_USERNAME);
    verifyNoInteractions(mockVwbUserService, mockInitialCreditsService);
  }

  @Test
  void processCreateVwbPodTask_whenUserExistsButAlreadyHasPod_returnsNoContent() {
    // Arrange
    mockFeatureFlags.enableVWBUserAndPodCreation = true;
    CreateVwbPodTaskRequest request = new CreateVwbPodTaskRequest();
    request.setUserName(TEST_USERNAME);

    DbUser existingUser = new DbUser();
    existingUser.setUsername(TEST_USERNAME);

    DbVwbUserPod dbVwbUserPod1 =
        new DbVwbUserPod()
            .setVwbUserPodId(1L)
            .setUser(existingUser)
            .setVwbPodId("pod1")
            .setInitialCreditsActive(true);

    existingUser.setVwbUserPod(dbVwbUserPod1);

    when(mockUserService.getByUsername(TEST_USERNAME)).thenReturn(Optional.of(existingUser));

    // Act
    ResponseEntity<Void> response = controller.processCreateVwbPodTask(request);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    verify(mockUserService).getByUsername(TEST_USERNAME);
    verifyNoInteractions(mockVwbUserService, mockInitialCreditsService);
  }

  @Test
  void processCreateVwbPodTask_whenUserExistsWithoutPod_createsPodAndReturnsOk() {
    // Arrange
    mockFeatureFlags.enableVWBUserAndPodCreation = true;
    CreateVwbPodTaskRequest request = new CreateVwbPodTaskRequest();
    request.setUserName(TEST_USERNAME);

    DbUser user = new DbUser();
    user.setUsername(TEST_USERNAME);
    user.setVwbUserPod(null);

    when(mockUserService.getByUsername(TEST_USERNAME)).thenReturn(Optional.of(user));
    when(mockInitialCreditsService.hasUserRunOutOfInitialCredits(user)).thenReturn(false);
    when(mockInitialCreditsService.areUserCreditsExpired(user)).thenReturn(false);

    // Act
    ResponseEntity<Void> response = controller.processCreateVwbPodTask(request);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    verify(mockUserService).getByUsername(TEST_USERNAME);
    verify(mockVwbUserService).createInitialCreditsPodForUser(user);
    verify(mockInitialCreditsService).hasUserRunOutOfInitialCredits(user);
    verify(mockInitialCreditsService).areUserCreditsExpired(user);
    verify(mockVwbUserService, never()).unlinkBillingAccountForUserPod(any());
  }

  @Test
  void
      processCreateVwbPodTask_whenUserExistsWithoutPodAndCreditsExhausted_createsPodUnlinksBillingAndReturnsOk() {
    // Arrange
    mockFeatureFlags.enableVWBUserAndPodCreation = true;
    CreateVwbPodTaskRequest request = new CreateVwbPodTaskRequest();
    request.setUserName(TEST_USERNAME);

    DbUser user = new DbUser();
    user.setUsername(TEST_USERNAME);
    user.setVwbUserPod(null);

    when(mockUserService.getByUsername(TEST_USERNAME)).thenReturn(Optional.of(user));
    when(mockInitialCreditsService.hasUserRunOutOfInitialCredits(user)).thenReturn(true);

    // Act
    ResponseEntity<Void> response = controller.processCreateVwbPodTask(request);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    verify(mockUserService).getByUsername(TEST_USERNAME);
    verify(mockVwbUserService).createInitialCreditsPodForUser(user);
    verify(mockInitialCreditsService).hasUserRunOutOfInitialCredits(user);
    verify(mockVwbUserService).unlinkBillingAccountForUserPod(user);
  }

  @Test
  void
      processCreateVwbPodTask_whenUserExistsWithoutPodAndCreditsExpired_createsPodUnlinksBillingAndReturnsOk() {
    // Arrange
    mockFeatureFlags.enableVWBUserAndPodCreation = true;
    CreateVwbPodTaskRequest request = new CreateVwbPodTaskRequest();
    request.setUserName(TEST_USERNAME);

    DbUser user = new DbUser();
    user.setUsername(TEST_USERNAME);
    user.setVwbUserPod(null);

    when(mockUserService.getByUsername(TEST_USERNAME)).thenReturn(Optional.of(user));
    when(mockInitialCreditsService.hasUserRunOutOfInitialCredits(user)).thenReturn(false);
    when(mockInitialCreditsService.areUserCreditsExpired(user)).thenReturn(true);

    // Act
    ResponseEntity<Void> response = controller.processCreateVwbPodTask(request);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    verify(mockUserService).getByUsername(TEST_USERNAME);
    verify(mockVwbUserService).createInitialCreditsPodForUser(user);
    verify(mockInitialCreditsService).hasUserRunOutOfInitialCredits(user);
    verify(mockInitialCreditsService).areUserCreditsExpired(user);
    verify(mockVwbUserService).unlinkBillingAccountForUserPod(user);
  }

  @Test
  void
      processCreateVwbPodTask_whenUserExistsWithoutPodAndCreditsExhaustedAndExpired_createsPodUnlinksBillingAndReturnsOk() {
    // Arrange
    mockFeatureFlags.enableVWBUserAndPodCreation = true;
    CreateVwbPodTaskRequest request = new CreateVwbPodTaskRequest();
    request.setUserName(TEST_USERNAME);

    DbUser user = new DbUser();
    user.setUsername(TEST_USERNAME);
    user.setVwbUserPod(null);

    when(mockUserService.getByUsername(TEST_USERNAME)).thenReturn(Optional.of(user));
    when(mockInitialCreditsService.hasUserRunOutOfInitialCredits(user)).thenReturn(true);

    // Act
    ResponseEntity<Void> response = controller.processCreateVwbPodTask(request);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    verify(mockUserService).getByUsername(TEST_USERNAME);
    verify(mockVwbUserService).createInitialCreditsPodForUser(user);
    verify(mockInitialCreditsService).hasUserRunOutOfInitialCredits(user);
    verify(mockVwbUserService).unlinkBillingAccountForUserPod(user);
  }
}
