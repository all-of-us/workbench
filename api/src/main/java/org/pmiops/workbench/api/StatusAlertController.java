package org.pmiops.workbench.api;


import java.util.List;
import java.util.stream.Collectors;
import org.pmiops.workbench.db.dao.StatusAlertDao;
import org.pmiops.workbench.db.model.DbStatusAlert;
import org.pmiops.workbench.model.StatusAlert;
import org.pmiops.workbench.model.StatusAlertResponse;
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
  public ResponseEntity<StatusAlertResponse> getStatusAlert() {
    List<DbStatusAlert> dbStatusAlerts = statusAlertDao.findAll();
    List<StatusAlert> statusAlerts = dbStatusAlerts.stream()
        .map(
            dbStatusAlert -> new StatusAlert()
                .statusAlertId(dbStatusAlert.getStatusAlertId())
                .title(dbStatusAlert.getTitle())
                .message(dbStatusAlert.getMessage())
                .link(dbStatusAlert.getLink())
        ).collect(Collectors.toList());
    StatusAlertResponse statusAlertResponse = new StatusAlertResponse();
    statusAlertResponse.addAll(statusAlerts);
    return ResponseEntity.ok(statusAlertResponse);
  }

}
