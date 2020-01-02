package org.pmiops.workbench.statusalerts;

import java.util.List;
import java.util.Optional;

import org.pmiops.workbench.annotations.AuthorityRequired;
import org.pmiops.workbench.api.StatusAlertApiDelegate;
import org.pmiops.workbench.db.dao.StatusAlertDao;
import org.pmiops.workbench.db.model.DbStatusAlert;
import org.pmiops.workbench.model.Authority;
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
    Optional<DbStatusAlert> dbStatusAlertOptional =
        statusAlertDao.findFirstByOrderByStatusAlertIdDesc();
    if (dbStatusAlertOptional.isPresent()) {
      DbStatusAlert dbStatusAlert = dbStatusAlertOptional.get();
      return ResponseEntity.ok(StatusAlertConversionUtils.toApiStatusAlert(dbStatusAlert));
    } else {
      return ResponseEntity.ok(new StatusAlert());
    }
  }

  @Override
  @AuthorityRequired(Authority.COMMUNICATIONS_ADMIN)
  public ResponseEntity<StatusAlert> postStatusAlert(StatusAlert statusAlert) {
    // Current assumption is only one banner at a time, therefore, we must delete all old banners before saving
    // a new one.
    Iterable<DbStatusAlert> dbStatusAlertList = statusAlertDao.findAll();
    dbStatusAlertList.forEach(statusAlert1 -> statusAlertDao.delete(statusAlert1.getStatusAlertId()));
    // If we support multiple banners at a time, the above lines can be deleted.

    DbStatusAlert dbStatusAlert = statusAlertDao.save(StatusAlertConversionUtils.toDbStatusAlert(statusAlert));
    return ResponseEntity.ok(StatusAlertConversionUtils.toApiStatusAlert(dbStatusAlert));
  }
}
