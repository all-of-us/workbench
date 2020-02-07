package org.pmiops.workbench.mappers;

import java.sql.Timestamp;
import java.util.Optional;
import org.mapstruct.Mapper;
import org.pmiops.workbench.db.model.CommonStorageEnums;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.model.DataAccessLevel;

@Mapper(componentModel = "spring")
public class CommonMappers {

  public static Long timestamp(Timestamp timestamp) {
    if (timestamp != null) {
      return timestamp.getTime();
    }

    return null;
  }

  public static Timestamp timestamp(Long timestamp) {
    if (timestamp != null) {
      return new Timestamp(timestamp);
    }

    return null;
  }

  public static String dbUserToCreatorEmail(DbUser creator) {
    return creator.getUsername();
  }

  public static String cdrVersionToId(DbCdrVersion cdrVersion) {
    return Optional.ofNullable(cdrVersion)
        .map(DbCdrVersion::getCdrVersionId)
        .map(id -> Long.toString(id))
        .orElse(null);
  }

  public static Short dataAccessLevelToStorageEnum(DataAccessLevel dataAccessLevel) {
    return CommonStorageEnums.dataAccessLevelToStorage(dataAccessLevel);
  }
}
