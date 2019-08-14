package org.pmiops.workbench.utils;

import java.sql.Timestamp;
import org.mapstruct.Mapper;
import org.mapstruct.Named;
import org.pmiops.workbench.api.Etags;

@Mapper(componentModel = "spring")
public class CommonMappers {

  public static String enumToString(Enum<?> source) {
    return source == null ? null : source.toString();
  }

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

  @Named("etag")
  public String etag(int version) {
    return Etags.fromVersion(version);
  }
}
