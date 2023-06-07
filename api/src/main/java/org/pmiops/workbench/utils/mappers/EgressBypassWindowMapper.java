package org.pmiops.workbench.utils.mappers;

import com.google.gson.Gson;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.pmiops.workbench.db.model.DbEgressEvent;
import org.pmiops.workbench.db.model.DbEgressEvent.DbEgressEventStatus;
import org.pmiops.workbench.db.model.DbUserEgressBypassWindow;
import org.pmiops.workbench.model.EgressBypassWindow;
import org.pmiops.workbench.model.EgressEvent;
import org.pmiops.workbench.model.EgressEventStatus;
import org.pmiops.workbench.model.SumologicEgressEvent;

@Mapper(config = MapStructConfig.class, uses = CommonMappers.class)
public interface EgressBypassWindowMapper {

  @Mapping(target = "egressBypassId", ignore = true)
  @Mapping(target = "userId", ignore = true)
  EgressBypassWindow toApiEgressBypassWindow(DbUserEgressBypassWindow bypassWindow);
}
