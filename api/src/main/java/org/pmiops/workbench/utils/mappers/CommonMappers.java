package org.pmiops.workbench.utils.mappers;

import java.sql.Timestamp;
import java.util.Optional;
import org.mapstruct.Mapper;
import org.mapstruct.Named;
import org.pmiops.workbench.api.Etags;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbStorageEnums;
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
    return Optional.ofNullable(creator).map(DbUser::getUsername).orElse(null);
  }

  public static String cdrVersionToId(DbCdrVersion cdrVersion) {
    return Optional.ofNullable(cdrVersion)
        .map(DbCdrVersion::getCdrVersionId)
        .map(id -> Long.toString(id))
        .orElse(null);
  }

  public static Short dataAccessLevelToStorageEnum(DataAccessLevel dataAccessLevel) {
    return DbStorageEnums.dataAccessLevelToStorage(dataAccessLevel);
  }

  @Named("cdrVersionToEtag")
  public static String cdrVersionToEtag(int cdrVersion) {
    return Etags.fromVersion(cdrVersion);
  }

  @Named("etagToCdrVersion")
  public static int etagToCdrVersion(String etag) {
    return Etags.toVersion(etag);
  }
}
