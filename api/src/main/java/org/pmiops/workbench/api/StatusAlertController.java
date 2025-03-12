package org.pmiops.workbench.api;

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
        statusAlertDao
            .findFirstByOrderByStatusAlertIdDesc()
            .map(statusAlertMapper::toStatusAlert)
            .orElse(new StatusAlert()));
  }

  @Override
  @AuthorityRequired(Authority.COMMUNICATIONS_ADMIN)
  public ResponseEntity<StatusAlert> postStatusAlert(StatusAlert statusAlert) {
    // Current assumption is only one banner at a time, therefore, we must delete all old banners
    // before saving
    // a new one.
    Iterable<DbStatusAlert> dbStatusAlertList = statusAlertDao.findAll();
    dbStatusAlertList.forEach(
        statusAlert1 -> statusAlertDao.deleteById(statusAlert1.getStatusAlertId()));
    // If we support multiple banners at a time, the above lines can be deleted.

    DbStatusAlert dbStatusAlert =
        statusAlertDao.save(statusAlertMapper.toDbStatusAlert(statusAlert));
    return ResponseEntity.ok(statusAlertMapper.toStatusAlert(dbStatusAlert));
  }
}
