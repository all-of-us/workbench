package org.pmiops.workbench.utils.mappers;

import com.google.common.base.Strings;
import jakarta.annotation.Nullable;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.mapstruct.Context;
import org.mapstruct.Named;
import org.pmiops.workbench.api.Etags;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.initialcredits.InitialCreditsExpirationService;
import org.pmiops.workbench.model.Domain;
import org.springframework.stereotype.Service;

@Service
public class CommonMappers {

  private final Clock clock;

  public CommonMappers(Clock clock) {
    this.clock = clock;
  }

  public Long timestamp(Timestamp timestamp) {
    if (timestamp != null) {
      return timestamp.getTime();
    }
    return null;
  }

  public static OffsetDateTime offsetDateTimeUtc(Timestamp timestamp) {
    return Optional.ofNullable(timestamp)
        .map(Timestamp::toInstant)
        .map(instant -> OffsetDateTime.ofInstant(instant, ZoneOffset.UTC))
        .orElse(null);
  }

  public static Timestamp timestamp(OffsetDateTime offsetDateTime) {
    return Optional.ofNullable(offsetDateTime)
        .map(OffsetDateTime::toInstant)
        .map(Timestamp::from)
        .orElse(null);
  }

  @Named("toTimestampCurrentIfNull")
  public Timestamp timestampCurrentIfNull(Long timestamp) {
    if (timestamp != null) {
      return new Timestamp(timestamp);
    }
    return Timestamp.from(clock.instant());
  }

  public String timestampToString(Timestamp timestamp) {
    // We are using this method because mapstruct defaults to gregorian conversion. The difference
    // is:
    // Gregorian: "2020-03-30T18:31:50.000Z"
    // toString: "2020-03-30 18:31:50.0"
    if (timestamp != null) {
      return timestamp.toString();
    }
    return null;
  }

  public Timestamp timestamp(Long timestamp) {
    if (timestamp != null) {
      return new Timestamp(timestamp);
    }

    return null;
  }

  @Named("timestampToIso8601String")
  public String timestampToIso8601String(Timestamp timestamp) {
    if (timestamp == null) {
      return null;
    }
    return timestamp.toInstant().toString();
  }

  @Named("dateToString")
  public String dateToString(Date date) {
    // We are using this method because mapstruct defaults to gregorian conversion. The difference
    // is:
    // Gregorian: "2020-03-30T18:31:50.000Z"
    // toString: "2020-03-30"
    if (date != null) {
      return date.toString();
    }
    return null;
  }

  public String dbUserToCreatorEmail(DbUser creator) {
    return Optional.ofNullable(creator).map(DbUser::getUsername).orElse(null);
  }

  public String cdrVersionToId(DbCdrVersion cdrVersion) {
    return Optional.ofNullable(cdrVersion)
        .map(DbCdrVersion::getCdrVersionId)
        .map(id -> Long.toString(id))
        .orElse(null);
  }

  @Named("versionToEtag")
  public String versionToEtag(int version) {
    return Etags.fromVersion(version);
  }

  @Named("etagToCdrVersion")
  public int etagToCdrVersion(String etag) {
    return Strings.isNullOrEmpty(etag) ? 1 : Etags.toVersion(etag);
  }

  @Named("domainIdToDomain")
  public Domain domainIdToDomain(String domainId) {
    return Enum.valueOf(Domain.class, domainId);
  }

  @Named("getInitialCreditsExpiration")
  @Nullable
  public Long getInitialCreditsExpiration(
      DbUser source, @Context InitialCreditsExpirationService expirationService) {
    return expirationService.getCreditsExpiration(source).map(this::timestamp).orElse(null);
  }
}
