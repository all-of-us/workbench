package org.pmiops.workbench.api;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.pmiops.workbench.annotations.AuthorityRequired;
import org.pmiops.workbench.model.Authority;
import org.pmiops.workbench.model.VwbSystemNotification;
import org.pmiops.workbench.model.VwbSystemNotificationPriority;
import org.pmiops.workbench.model.VwbSystemNotificationType;
import org.pmiops.workbench.vwb.user.model.NotificationDescription;
import org.pmiops.workbench.vwb.user.model.NotificationPriority;
import org.pmiops.workbench.vwb.user.model.NotificationType;
import org.pmiops.workbench.vwb.usermanager.VwbUserManagerClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Manages Verily Workbench notifications shown to All of Us users. Verily Workbench is the system
 * of record: we read and delete notifications through its API rather than keeping our own copy, so
 * this page cannot drift from what users actually see.
 */
@RestController
public class VwbSystemNotificationAdminController implements VwbSystemNotificationAdminApiDelegate {

  /** VWB caps its notification listing at 100 per request. */
  private static final int LIST_LIMIT = 100;

  private final VwbUserManagerClient vwbUserManagerClient;

  @Autowired
  public VwbSystemNotificationAdminController(VwbUserManagerClient vwbUserManagerClient) {
    this.vwbUserManagerClient = vwbUserManagerClient;
  }

  @Override
  @AuthorityRequired(Authority.COMMUNICATIONS_ADMIN)
  public ResponseEntity<List<VwbSystemNotification>> listVwbSystemNotifications() {
    return ResponseEntity.ok(
        vwbUserManagerClient.listOrganizationNotifications(LIST_LIMIT).stream()
            .map(VwbSystemNotificationAdminController::toApi)
            .toList());
  }

  @Override
  @AuthorityRequired(Authority.COMMUNICATIONS_ADMIN)
  public ResponseEntity<VwbSystemNotification> createVwbSystemNotification(
      VwbSystemNotification request) {
    return ResponseEntity.ok(
        toApi(
            vwbUserManagerClient.createOrganizationNotification(
                request.getTitle(),
                request.getMessage(),
                NotificationType.fromValue(request.getNotificationType().toString()),
                NotificationPriority.fromValue(request.getNotificationPriority().toString()),
                toInstant(request.getStartTimeEpochMillis()),
                toInstant(request.getEndTimeEpochMillis()))));
  }

  @Override
  @AuthorityRequired(Authority.COMMUNICATIONS_ADMIN)
  public ResponseEntity<Void> deleteVwbSystemNotification(String id) {
    vwbUserManagerClient.deleteNotification(id);
    return ResponseEntity.noContent().build();
  }

  private static VwbSystemNotification toApi(NotificationDescription notification) {
    return new VwbSystemNotification()
        .id(Optional.ofNullable(notification.getId()).map(Object::toString).orElse(null))
        .title(notification.getTitle())
        .message(notification.getMessage())
        .notificationType(toApiType(notification.getNotificationType()))
        .notificationPriority(toApiPriority(notification.getNotificationPriority()))
        .startTimeEpochMillis(toEpochMillis(notification.getStartTime()))
        .endTimeEpochMillis(toEpochMillis(notification.getEndTime()));
  }

  private static VwbSystemNotificationType toApiType(NotificationType type) {
    return type == null ? null : VwbSystemNotificationType.fromValue(type.toString());
  }

  private static VwbSystemNotificationPriority toApiPriority(NotificationPriority priority) {
    return priority == null ? null : VwbSystemNotificationPriority.fromValue(priority.toString());
  }

  private static Instant toInstant(Long epochMillis) {
    return epochMillis == null ? null : Instant.ofEpochMilli(epochMillis);
  }

  private static Long toEpochMillis(OffsetDateTime dateTime) {
    return dateTime == null ? null : dateTime.toInstant().toEpochMilli();
  }
}
