package org.pmiops.workbench.utils.mappers;

import static org.pmiops.workbench.utils.BillingUtils.isInitialCredits;

import com.google.common.base.Strings;
import jakarta.annotation.Nullable;
import jakarta.inject.Provider;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.mapstruct.Context;
import org.mapstruct.Named;
import org.pmiops.workbench.api.Etags;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.initialcredits.InitialCreditsService;
import org.pmiops.workbench.model.BillingStatus;
import org.pmiops.workbench.model.Domain;
import org.springframework.stereotype.Service;

@Service
public class CommonMappers {

  private final Clock clock;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;

  public CommonMappers(Clock clock, Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.clock = clock;
    this.workbenchConfigProvider = workbenchConfigProvider;
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
        .map(odt -> odt.atZoneSameInstant(Clock.systemDefaultZone().getZone()).toOffsetDateTime())
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
      DbUser source, @Context InitialCreditsService initialCreditsService) {
    return initialCreditsService.getCreditsExpiration(source).map(this::timestamp).orElse(null);
  }

  @Named("getInitialCreditsExtension")
  @Nullable
  public Long getInitialCreditsExtension(
      DbUser source, @Context InitialCreditsService initialCreditsService) {
    return initialCreditsService.getCreditsExtension(source).map(this::timestamp).orElse(null);
  }

  @Named("getBillingStatus")
  public BillingStatus getBillingStatus(
      DbWorkspace dbWorkspace, @Context InitialCreditsService initialCreditsService) {
    return (isInitialCredits(dbWorkspace.getBillingAccountName(), workbenchConfigProvider.get())
            && (dbWorkspace.isInitialCreditsExhausted()
                || initialCreditsService.areUserCreditsExpired(dbWorkspace.getCreator())))
        ? BillingStatus.INACTIVE
        : BillingStatus.ACTIVE;
  }

  @Named("checkInitialCreditsExtensionEligibility")
  public boolean checkInitialCreditsExtensionEligibility(
      DbUser dbUser, @Context InitialCreditsService initialCreditsService) {
    return initialCreditsService.checkInitialCreditsExtensionEligibility(dbUser);
  }

  @Named("isInitialCreditExpirationBypassed")
  public boolean isInitialCreditExpirationBypassed(
      DbUser dbUser, @Context InitialCreditsService initialCreditsService) {
    return initialCreditsService.isExpirationBypassed(dbUser);
  }
}
