package org.pmiops.workbench.api;

import java.util.List;
import java.util.stream.Collectors;
import org.pmiops.workbench.annotations.AuthorityRequired;
import org.pmiops.workbench.db.dao.StatusAlertDao;
import org.pmiops.workbench.db.model.DbStatusAlert;
import org.pmiops.workbench.model.Authority;
import org.pmiops.workbench.model.StatusAlert;
import org.pmiops.workbench.statusalerts.StatusAlertMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatusAlertController implements StatusAlertApiDelegate {
  private final StatusAlertDao statusAlertDao;
  private final StatusAlertMapper statusAlertMapper;

  @Autowired
  StatusAlertController(StatusAlertDao statusAlertDao, StatusAlertMapper statusAlertMapper) {
    this.statusAlertDao = statusAlertDao;
    this.statusAlertMapper = statusAlertMapper;
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
}
