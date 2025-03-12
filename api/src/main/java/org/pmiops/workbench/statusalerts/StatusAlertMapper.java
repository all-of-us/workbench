package org.pmiops.workbench.statusalerts;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.pmiops.workbench.db.model.DbStatusAlert;
import org.pmiops.workbench.model.StatusAlert;
import org.pmiops.workbench.utils.mappers.MapStructConfig;

@Mapper(config = MapStructConfig.class)
public interface StatusAlertMapper {
  StatusAlert toStatusAlert(DbStatusAlert statusAlert);

  @Mapping(target = "title", expression = "java(statusAlert.getAlertLocation() == StatusAlertLocation.AFTER_LOGIN ? statusAlert.getTitle() : \"Scheduled Downtime Notice for the Researcher Workbench\")")
  DbStatusAlert toDbStatusAlert(StatusAlert statusAlert);
}
