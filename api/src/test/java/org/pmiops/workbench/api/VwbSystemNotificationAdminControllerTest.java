package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.model.VwbSystemNotification;
import org.pmiops.workbench.model.VwbSystemNotificationPriority;
import org.pmiops.workbench.model.VwbSystemNotificationType;
import org.pmiops.workbench.vwb.user.model.NotificationDescription;
import org.pmiops.workbench.vwb.user.model.NotificationPriority;
import org.pmiops.workbench.vwb.user.model.NotificationType;
import org.pmiops.workbench.vwb.usermanager.VwbUserManagerClient;

public class VwbSystemNotificationAdminControllerTest {
  private static final UUID VWB_NOTIFICATION_ID =
      UUID.fromString("b6a1c0f2-1234-4c5e-8a9b-0d1e2f3a4b5c");
  private static final String TITLE = "Scheduled maintenance";
  private static final String MESSAGE = "Verily Workbench will be unavailable on Saturday.";
  private static final Instant START_TIME = Instant.ofEpochMilli(1_760_000_000_000L);
  private static final Instant END_TIME = START_TIME.plusSeconds(3600);

  @Mock private VwbUserManagerClient vwbUserManagerClient;

  private VwbSystemNotificationAdminController controller;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    controller = new VwbSystemNotificationAdminController(vwbUserManagerClient);
  }

  private static NotificationDescription vwbNotification() {
    return new NotificationDescription()
        .id(VWB_NOTIFICATION_ID)
        .title(TITLE)
        .message(MESSAGE)
        .notificationType(NotificationType.BLOCKING)
        .notificationPriority(NotificationPriority.WARNING)
        .startTime(OffsetDateTime.ofInstant(START_TIME, ZoneOffset.UTC))
        .endTime(OffsetDateTime.ofInstant(END_TIME, ZoneOffset.UTC));
  }

  private static VwbSystemNotification request() {
    return new VwbSystemNotification()
        .title(TITLE)
        .message(MESSAGE)
        .notificationType(VwbSystemNotificationType.BLOCKING)
        .notificationPriority(VwbSystemNotificationPriority.WARNING);
  }

  @Test
  public void testCreate() {
    when(vwbUserManagerClient.createOrganizationNotification(
            any(), any(), any(), any(), any(), any()))
        .thenReturn(vwbNotification());

    VwbSystemNotification created =
        controller
            .createVwbSystemNotification(
                request()
                    .startTimeEpochMillis(START_TIME.toEpochMilli())
                    .endTimeEpochMillis(END_TIME.toEpochMilli()))
            .getBody();

    verify(vwbUserManagerClient)
        .createOrganizationNotification(
            TITLE,
            MESSAGE,
            NotificationType.BLOCKING,
            NotificationPriority.WARNING,
            START_TIME,
            END_TIME);

    // The ID Verily Workbench assigned is what the admin page deletes by.
    assertThat(created.getId()).isEqualTo(VWB_NOTIFICATION_ID.toString());
    assertThat(created.getTitle()).isEqualTo(TITLE);
    assertThat(created.getNotificationType()).isEqualTo(VwbSystemNotificationType.BLOCKING);
    assertThat(created.getNotificationPriority()).isEqualTo(VwbSystemNotificationPriority.WARNING);
    assertThat(created.getStartTimeEpochMillis()).isEqualTo(START_TIME.toEpochMilli());
    assertThat(created.getEndTimeEpochMillis()).isEqualTo(END_TIME.toEpochMilli());
  }

  @Test
  public void testCreate_nullTimesArePassedThrough() {
    when(vwbUserManagerClient.createOrganizationNotification(
            any(), any(), any(), any(), any(), any()))
        .thenReturn(vwbNotification().startTime(null).endTime(null));

    // Null start/end mean "shown immediately" and "never expires"; VWB applies its own defaults.
    VwbSystemNotification created = controller.createVwbSystemNotification(request()).getBody();

    verify(vwbUserManagerClient)
        .createOrganizationNotification(
            TITLE, MESSAGE, NotificationType.BLOCKING, NotificationPriority.WARNING, null, null);
    assertThat(created.getStartTimeEpochMillis()).isNull();
    assertThat(created.getEndTimeEpochMillis()).isNull();
  }

  @Test
  public void testCreate_vwbFailurePropagates() {
    doThrow(new ServerErrorException("user manager unavailable"))
        .when(vwbUserManagerClient)
        .createOrganizationNotification(any(), any(), any(), any(), any(), any());

    assertThrows(
        ServerErrorException.class, () -> controller.createVwbSystemNotification(request()));
  }

  @Test
  public void testList() {
    when(vwbUserManagerClient.listOrganizationNotifications(100))
        .thenReturn(List.of(vwbNotification()));

    List<VwbSystemNotification> listed = controller.listVwbSystemNotifications().getBody();

    assertThat(listed).hasSize(1);
    assertThat(listed.get(0).getId()).isEqualTo(VWB_NOTIFICATION_ID.toString());
    assertThat(listed.get(0).getMessage()).isEqualTo(MESSAGE);
  }

  @Test
  public void testList_empty() {
    when(vwbUserManagerClient.listOrganizationNotifications(100)).thenReturn(List.of());

    assertThat(controller.listVwbSystemNotifications().getBody()).isEmpty();
  }

  @Test
  public void testList_toleratesMissingTypeAndPriority() {
    // Both are optional in the Verily Workbench API, so a notification created elsewhere may
    // omit them.
    when(vwbUserManagerClient.listOrganizationNotifications(100))
        .thenReturn(List.of(vwbNotification().notificationType(null).notificationPriority(null)));

    VwbSystemNotification listed = controller.listVwbSystemNotifications().getBody().get(0);
    assertThat(listed.getNotificationType()).isNull();
    assertThat(listed.getNotificationPriority()).isNull();
  }

  @Test
  public void testDelete() {
    controller.deleteVwbSystemNotification(VWB_NOTIFICATION_ID.toString());

    verify(vwbUserManagerClient).deleteNotification(VWB_NOTIFICATION_ID.toString());
  }

  @Test
  public void testDelete_vwbFailurePropagates() {
    doThrow(new ServerErrorException("user manager unavailable"))
        .when(vwbUserManagerClient)
        .deleteNotification(any());

    assertThrows(
        ServerErrorException.class,
        () -> controller.deleteVwbSystemNotification(VWB_NOTIFICATION_ID.toString()));
  }
}
