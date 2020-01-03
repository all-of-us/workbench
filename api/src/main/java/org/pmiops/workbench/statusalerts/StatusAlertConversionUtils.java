package org.pmiops.workbench.statusalerts;

import org.pmiops.workbench.db.model.DbStatusAlert;
import org.pmiops.workbench.model.StatusAlert;

public class StatusAlertConversionUtils {
  public static StatusAlert toApiStatusAlert(DbStatusAlert statusAlert) {
    return new StatusAlert()
        .statusAlertId(statusAlert.getStatusAlertId())
        .title(statusAlert.getTitle())
        .message(statusAlert.getMessage())
        .link(statusAlert.getLink());
  }

  public static DbStatusAlert toDbStatusAlert(StatusAlert statusAlert) {
    DbStatusAlert dbStatusAlert = new DbStatusAlert();
    if (statusAlert.getStatusAlertId() != null) {
      dbStatusAlert.setStatusAlertId(statusAlert.getStatusAlertId());
    }
    dbStatusAlert.setLink(statusAlert.getLink());
    dbStatusAlert.setMessage(statusAlert.getMessage());
    dbStatusAlert.setTitle(statusAlert.getTitle());
    return dbStatusAlert;
  }
}
