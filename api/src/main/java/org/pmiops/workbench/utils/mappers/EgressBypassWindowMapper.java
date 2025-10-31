package org.pmiops.workbench.utils.mappers;

import java.util.UUID;
import org.mapstruct.Mapper;
import org.pmiops.workbench.db.model.DbUserEgressBypassWindow;
import org.pmiops.workbench.model.EgressBypassWindow;

@Mapper(config = MapStructConfig.class, uses = CommonMappers.class)
public interface EgressBypassWindowMapper {
  EgressBypassWindow toApiEgressBypassWindow(DbUserEgressBypassWindow bypassWindow);

  default UUID stringToUuid(String value) {
    return value != null ? UUID.fromString(value) : null;
  }
}
