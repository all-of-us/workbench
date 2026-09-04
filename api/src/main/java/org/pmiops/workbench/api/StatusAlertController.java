package org.pmiops.workbench.api;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.pmiops.workbench.annotations.AuthorityRequired;
import org.pmiops.workbench.db.dao.StatusAlertDao;
import org.pmiops.workbench.db.model.DbStatusAlert;
import org.pmiops.workbench.model.Authority;
import org.pmiops.workbench.model.StatusAlert;
import org.pmiops.workbench.model.VwbBanner;
import org.pmiops.workbench.statusalerts.StatusAlertMapper;
import org.pmiops.workbench.vwb.user.model.NotificationDescription;
import org.pmiops.workbench.vwb.user.model.NotificationPriority;
import org.pmiops.workbench.vwb.user.model.NotificationType;
import org.pmiops.workbench.vwb.usermanager.VwbUserManagerClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatusAlertController implements StatusAlertApiDelegate {
  private final StatusAlertDao statusAlertDao;
  private final StatusAlertMapper statusAlertMapper;
  private final VwbUserManagerClient vwbUserManagerClient;

  @Autowired
  StatusAlertController(
      StatusAlertDao statusAlertDao,
      StatusAlertMapper statusAlertMapper,
      VwbUserManagerClient vwbUserManagerClient) {
    this.statusAlertDao = statusAlertDao;
    this.statusAlertMapper = statusAlertMapper;
    this.vwbUserManagerClient = vwbUserManagerClient;
  }

  @Override
  public ResponseEntity<StatusAlert> getStatusAlert() {
    return ResponseEntity.ok(
        statusAlertDao.findAllByOrderByStatusAlertIdDesc().stream()
            .findFirst()
            .map(statusAlertMapper::toStatusAlert)
            .orElse(new StatusAlert()));
  }

  @Override
  public ResponseEntity<List<StatusAlert>> getStatusAlerts() {
    return ResponseEntity.ok(
        statusAlertDao.findAllByOrderByStatusAlertIdDesc().stream()
            .map(statusAlertMapper::toStatusAlert)
            .collect(Collectors.toList()));
  }

  @Override
  public ResponseEntity<Void> deleteStatusAlert(Long id) {
    statusAlertDao.deleteById(id);
    return ResponseEntity.noContent().build();
  }

  @Override
  @AuthorityRequired(Authority.COMMUNICATIONS_ADMIN)
  public ResponseEntity<StatusAlert> postStatusAlert(StatusAlert statusAlert) {
    DbStatusAlert dbStatusAlert =
        statusAlertDao.save(statusAlertMapper.toDbStatusAlert(statusAlert));
    return ResponseEntity.ok(statusAlertMapper.toStatusAlert(dbStatusAlert));
  }

  @Override
  @AuthorityRequired(Authority.COMMUNICATIONS_ADMIN)
  public ResponseEntity<VwbBanner> postVwbBanner(VwbBanner vwbBanner) {
    NotificationDescription created =
        vwbUserManagerClient.createOrganizationNotification(
            vwbBanner.getTitle(),
            vwbBanner.getMessage(),
            NotificationType.fromValue(vwbBanner.getNotificationType().toString()),
            NotificationPriority.fromValue(vwbBanner.getNotificationPriority().toString()),
            toInstant(vwbBanner.getStartTimeEpochMillis()),
            toInstant(vwbBanner.getEndTimeEpochMillis()));

    // The banner lives in VWB, so echo the request back with the ID VWB assigned rather than
    // remapping every field it returns.
    return ResponseEntity.ok(
        vwbBanner.id(Optional.ofNullable(created.getId()).map(Object::toString).orElse(null)));
  }

  private static Instant toInstant(Long epochMillis) {
    return epochMillis == null ? null : Instant.ofEpochMilli(epochMillis);
  }
}
