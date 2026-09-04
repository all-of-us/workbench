package org.pmiops.workbench.api;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.pmiops.workbench.annotations.AuthorityRequired;
import org.pmiops.workbench.db.dao.VwbSystemNotificationDao;
import org.pmiops.workbench.db.model.DbVwbSystemNotification;
import org.pmiops.workbench.exceptions.NotFoundException;
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
 * Manages Verily Workbench notifications shown to All of Us users. Verily Workbench owns the
 * notification itself; we keep a row per notification we created so admins can see what All of Us
 * put up and take it down again.
 */
@RestController
public class VwbSystemNotificationAdminController implements VwbSystemNotificationAdminApiDelegate {

  private final VwbUserManagerClient vwbUserManagerClient;
  private final VwbSystemNotificationDao vwbSystemNotificationDao;

  @Autowired
  public VwbSystemNotificationAdminController(
      VwbUserManagerClient vwbUserManagerClient,
      VwbSystemNotificationDao vwbSystemNotificationDao) {
    this.vwbUserManagerClient = vwbUserManagerClient;
    this.vwbSystemNotificationDao = vwbSystemNotificationDao;
  }

  @Override
  @AuthorityRequired(Authority.COMMUNICATIONS_ADMIN)
  public ResponseEntity<List<VwbSystemNotification>> listVwbSystemNotifications() {
    return ResponseEntity.ok(
        vwbSystemNotificationDao.findAllByOrderByVwbSystemNotificationIdDesc().stream()
            .map(VwbSystemNotificationAdminController::toApi)
            .toList());
  }

  @Override
  @AuthorityRequired(Authority.COMMUNICATIONS_ADMIN)
  public ResponseEntity<VwbSystemNotification> createVwbSystemNotification(
      VwbSystemNotification request) {
    // Create in VWB first: recording a row for a notification VWB rejected would show admins a
    // notification that no user can see.
    NotificationDescription created =
        vwbUserManagerClient.createOrganizationNotification(
            request.getTitle(),
            request.getMessage(),
            NotificationType.fromValue(request.getNotificationType().toString()),
            NotificationPriority.fromValue(request.getNotificationPriority().toString()),
            toInstant(request.getStartTimeEpochMillis()),
            toInstant(request.getEndTimeEpochMillis()));

    DbVwbSystemNotification saved =
        vwbSystemNotificationDao.save(
            new DbVwbSystemNotification()
                .setVwbNotificationId(created.getId().toString())
                .setTitle(request.getTitle())
                .setMessage(request.getMessage())
                .setNotificationType(request.getNotificationType().toString())
                .setNotificationPriority(request.getNotificationPriority().toString())
                .setStartTime(toTimestamp(request.getStartTimeEpochMillis()))
                .setEndTime(toTimestamp(request.getEndTimeEpochMillis())));

    return ResponseEntity.ok(toApi(saved));
  }

  @Override
  @AuthorityRequired(Authority.COMMUNICATIONS_ADMIN)
  public ResponseEntity<Void> deleteVwbSystemNotification(Long id) {
    DbVwbSystemNotification notification =
        vwbSystemNotificationDao
            .findById(id)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        String.format("Verily Workbench system notification %d not found", id)));

    // Delete in VWB first: dropping our row while the notification is still up in VWB would leave
    // it visible to users with no way for an admin to find it again.
    vwbUserManagerClient.deleteNotification(notification.getVwbNotificationId());
    vwbSystemNotificationDao.delete(notification);

    return ResponseEntity.noContent().build();
  }

  private static VwbSystemNotification toApi(DbVwbSystemNotification db) {
    return new VwbSystemNotification()
        .vwbSystemNotificationId(db.getVwbSystemNotificationId())
        .vwbNotificationId(db.getVwbNotificationId())
        .title(db.getTitle())
        .message(db.getMessage())
        .notificationType(VwbSystemNotificationType.fromValue(db.getNotificationType()))
        .notificationPriority(VwbSystemNotificationPriority.fromValue(db.getNotificationPriority()))
        .startTimeEpochMillis(toEpochMillis(db.getStartTime()))
        .endTimeEpochMillis(toEpochMillis(db.getEndTime()));
  }

  private static Instant toInstant(Long epochMillis) {
    return epochMillis == null ? null : Instant.ofEpochMilli(epochMillis);
  }

  private static Timestamp toTimestamp(Long epochMillis) {
    return epochMillis == null ? null : new Timestamp(epochMillis);
  }

  private static Long toEpochMillis(Timestamp timestamp) {
    return Optional.ofNullable(timestamp).map(Timestamp::getTime).orElse(null);
  }
}
