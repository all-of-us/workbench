package org.pmiops.workbench.utils.mappers;

import java.sql.Timestamp;
import org.mapstruct.Mapper;
import org.pmiops.workbench.db.model.CdrVersion;
import org.pmiops.workbench.db.model.CommonStorageEnums;
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
    return creator.getEmail();
  }

  public static String cdrVersionToId(CdrVersion cdrVersion) {
    return Long.toString(cdrVersion.getCdrVersionId());
  }

  public static Short dataAccessLevelToStorageEnum(DataAccessLevel dataAccessLevel) {
    return CommonStorageEnums.dataAccessLevelToStorage(dataAccessLevel);
  }
}
