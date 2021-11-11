package org.pmiops.workbench.utils.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.pmiops.workbench.db.model.DbEgressEvent;
import org.pmiops.workbench.db.model.DbEgressEvent.DbEgressEventStatus;
import org.pmiops.workbench.model.EgressEvent;
import org.pmiops.workbench.model.EgressEventStatus;

@Mapper(config = MapStructConfig.class, uses = CommonMappers.class)
public interface EgressEventMapper {

  @Mapping(target = "sourceUserEmail", source = "user.username")
  @Mapping(target = "sourceWorkspaceNamespace", source = "workspace.workspaceNamespace")
  @Mapping(target = "sourceGoogleProject", source = "workspace.googleProject")
  EgressEvent toApiEvent(DbEgressEvent event);

  EgressEventStatus toApiStatus(DbEgressEventStatus status);

  DbEgressEventStatus toDbStatus(EgressEventStatus status);
}
