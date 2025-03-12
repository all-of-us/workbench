package org.pmiops.workbench.statusalerts;

import org.pmiops.workbench.db.model.DbStatusAlert;
import org.pmiops.workbench.db.model.DbStatusAlert.DbAlertLocation;
import org.pmiops.workbench.model.StatusAlert;
import org.pmiops.workbench.model.StatusAlertLocation;

public class StatusAlertConversionUtils {
  public static StatusAlert toApiStatusAlert(DbStatusAlert statusAlert) {
    return new StatusAlert()
        .statusAlertId(statusAlert.getStatusAlertId())
        .title(statusAlert.getTitle())
        .message(statusAlert.getMessage())
        .link(statusAlert.getLink())
        .alertLocation(StatusAlertLocation.valueOf(statusAlert.getAlertLocation().toString()));
  }

  public static DbStatusAlert toDbStatusAlert(StatusAlert statusAlert) {
    DbStatusAlert dbStatusAlert = new DbStatusAlert();
    if (statusAlert.getStatusAlertId() != null) {
      dbStatusAlert.setStatusAlertId(statusAlert.getStatusAlertId());
    }
    dbStatusAlert.setLink(statusAlert.getLink());
    dbStatusAlert.setMessage(statusAlert.getMessage());
    dbStatusAlert.setTitle(
        statusAlert.getAlertLocation().equals(StatusAlertLocation.AFTER_LOGIN)
            ? statusAlert.getTitle()
            : "Scheduled Downtime Notice for the Researcher Workbench");
    dbStatusAlert.setAlertLocation(DbAlertLocation.valueOf(statusAlert.getAlertLocation().name()));
    return dbStatusAlert;
  }
}
