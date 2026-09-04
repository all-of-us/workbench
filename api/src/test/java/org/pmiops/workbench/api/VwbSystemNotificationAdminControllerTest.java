package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.db.dao.VwbSystemNotificationDao;
import org.pmiops.workbench.db.model.DbVwbSystemNotification;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.model.VwbSystemNotification;
import org.pmiops.workbench.model.VwbSystemNotificationPriority;
import org.pmiops.workbench.model.VwbSystemNotificationType;
import org.pmiops.workbench.vwb.user.model.NotificationDescription;
import org.pmiops.workbench.vwb.user.model.NotificationPriority;
import org.pmiops.workbench.vwb.user.model.NotificationType;
import org.pmiops.workbench.vwb.usermanager.VwbUserManagerClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@DataJpaTest
public class VwbSystemNotificationAdminControllerTest {
  private static final String VWB_NOTIFICATION_ID = "b6a1c0f2-1234-4c5e-8a9b-0d1e2f3a4b5c";
  private static final String TITLE = "Scheduled maintenance";
  private static final String MESSAGE = "Verily Workbench will be unavailable on Saturday.";

  @Autowired private VwbSystemNotificationAdminController controller;
  @Autowired private VwbSystemNotificationDao vwbSystemNotificationDao;

  @MockitoBean private VwbUserManagerClient vwbUserManagerClient;

  @TestConfiguration
  @Import({FakeClockConfiguration.class, VwbSystemNotificationAdminController.class})
  static class Configuration {}

  private void stubVwbCreate() {
    when(vwbUserManagerClient.createOrganizationNotification(
            any(), any(), any(), any(), any(), any()))
        .thenReturn(new NotificationDescription().id(UUID.fromString(VWB_NOTIFICATION_ID)));
  }

  private VwbSystemNotification request() {
    return new VwbSystemNotification()
        .title(TITLE)
        .message(MESSAGE)
        .notificationType(VwbSystemNotificationType.BLOCKING)
        .notificationPriority(VwbSystemNotificationPriority.WARNING);
  }

  @Test
  public void testCreate() {
    stubVwbCreate();
    Instant startTime = Instant.ofEpochMilli(1_760_000_000_000L);
    Instant endTime = startTime.plusSeconds(3600);

    VwbSystemNotification created =
        controller
            .createVwbSystemNotification(
                request()
                    .startTimeEpochMillis(startTime.toEpochMilli())
                    .endTimeEpochMillis(endTime.toEpochMilli()))
            .getBody();

    verify(vwbUserManagerClient)
        .createOrganizationNotification(
            TITLE,
            MESSAGE,
            NotificationType.BLOCKING,
            NotificationPriority.WARNING,
            startTime,
            endTime);

    // The VWB notification ID is recorded so the notification can be deleted there later.
    assertThat(created.getVwbNotificationId()).isEqualTo(VWB_NOTIFICATION_ID);
    assertThat(created.getVwbSystemNotificationId()).isNotNull();
    assertThat(created.getTitle()).isEqualTo(TITLE);
    assertThat(created.getStartTimeEpochMillis()).isEqualTo(startTime.toEpochMilli());
    assertThat(created.getEndTimeEpochMillis()).isEqualTo(endTime.toEpochMilli());

    DbVwbSystemNotification saved =
        vwbSystemNotificationDao.findById(created.getVwbSystemNotificationId()).orElseThrow();
    assertThat(saved.getVwbNotificationId()).isEqualTo(VWB_NOTIFICATION_ID);
    assertThat(saved.getNotificationType()).isEqualTo("BLOCKING");
    assertThat(saved.getNotificationPriority()).isEqualTo("WARNING");
  }

  @Test
  public void testCreate_nullTimesArePassedThrough() {
    stubVwbCreate();

    // Null start/end mean "shown immediately" and "never expires"; VWB applies its own defaults.
    VwbSystemNotification created = controller.createVwbSystemNotification(request()).getBody();

    verify(vwbUserManagerClient)
        .createOrganizationNotification(
            TITLE, MESSAGE, NotificationType.BLOCKING, NotificationPriority.WARNING, null, null);
    assertThat(created.getStartTimeEpochMillis()).isNull();
    assertThat(created.getEndTimeEpochMillis()).isNull();
  }

  @Test
  public void testCreate_vwbFailure_doesNotRecordNotification() {
    doThrow(new ServerErrorException("user manager unavailable"))
        .when(vwbUserManagerClient)
        .createOrganizationNotification(any(), any(), any(), any(), any(), any());

    assertThrows(
        ServerErrorException.class, () -> controller.createVwbSystemNotification(request()));

    // A notification VWB never created must not be listed for admins.
    assertThat(controller.listVwbSystemNotifications().getBody()).isEmpty();
  }

  @Test
  public void testList_mostRecentFirst() {
    DbVwbSystemNotification first = save("first", "id-1");
    DbVwbSystemNotification second = save("second", "id-2");

    List<VwbSystemNotification> listed = controller.listVwbSystemNotifications().getBody();

    assertThat(listed).hasSize(2);
    assertThat(listed.get(0).getVwbSystemNotificationId())
        .isEqualTo(second.getVwbSystemNotificationId());
    assertThat(listed.get(1).getVwbSystemNotificationId())
        .isEqualTo(first.getVwbSystemNotificationId());
  }

  @Test
  public void testDelete() {
    DbVwbSystemNotification saved = save(TITLE, VWB_NOTIFICATION_ID);

    controller.deleteVwbSystemNotification(saved.getVwbSystemNotificationId());

    verify(vwbUserManagerClient).deleteNotification(VWB_NOTIFICATION_ID);
    assertThat(controller.listVwbSystemNotifications().getBody()).isEmpty();
  }

  @Test
  public void testDelete_vwbFailure_keepsNotification() {
    DbVwbSystemNotification saved = save(TITLE, VWB_NOTIFICATION_ID);
    doThrow(new ServerErrorException("user manager unavailable"))
        .when(vwbUserManagerClient)
        .deleteNotification(any());

    assertThrows(
        ServerErrorException.class,
        () -> controller.deleteVwbSystemNotification(saved.getVwbSystemNotificationId()));

    // The notification is still up in VWB, so admins must still be able to find and retry it.
    assertThat(controller.listVwbSystemNotifications().getBody()).hasSize(1);
  }

  @Test
  public void testDelete_unknownId() {
    assertThrows(NotFoundException.class, () -> controller.deleteVwbSystemNotification(404L));
    verify(vwbUserManagerClient, never()).deleteNotification(any());
  }

  private DbVwbSystemNotification save(String title, String vwbNotificationId) {
    return vwbSystemNotificationDao.save(
        new DbVwbSystemNotification()
            .setVwbNotificationId(vwbNotificationId)
            .setTitle(title)
            .setMessage(MESSAGE)
            .setNotificationType("PASSIVE")
            .setNotificationPriority("INFO")
            .setStartTime(new Timestamp(1_760_000_000_000L)));
  }
}
