package org.pmiops.workbench.api;

import java.util.Optional;
import org.pmiops.workbench.db.dao.StatusAlertDao;
import org.pmiops.workbench.db.model.DbStatusAlert;
import org.pmiops.workbench.model.StatusAlert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatusAlertController implements StatusAlertApiDelegate {
  private final StatusAlertDao statusAlertDao;

  @Autowired
  StatusAlertController(StatusAlertDao statusAlertDao) {
    this.statusAlertDao = statusAlertDao;
  }

  @Override
  public ResponseEntity<StatusAlert> getStatusAlert() {
    Optional<DbStatusAlert> dbStatusAlertOptional = statusAlertDao.findFirstByOrderByStatusAlertIdDesc();
    if (dbStatusAlertOptional.isPresent()) {
      DbStatusAlert dbStatusAlert = dbStatusAlertOptional.get();
      StatusAlert statusAlert = new StatusAlert()
          .statusAlertId(dbStatusAlert.getStatusAlertId())
          .title(dbStatusAlert.getTitle())
          .message(dbStatusAlert.getMessage())
          .link(dbStatusAlert.getLink());
      return ResponseEntity.ok(statusAlert);
    }
    else {
      return ResponseEntity.noContent().build();
    }
  }

}
