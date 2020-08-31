package org.pmiops.workbench.utils.mappers;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import javax.inject.Provider;
import joptsimple.internal.Strings;
import org.mapstruct.Named;
import org.pmiops.workbench.api.Etags;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.model.BillingStatus;
import org.springframework.stereotype.Service;

@Service
public class CommonMappers {

  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final Clock clock;

  public CommonMappers(Provider<WorkbenchConfig> workbenchConfigProvider, Clock clock) {
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.clock = clock;
  }

  public String dbUserToCreatorEmail(DbUser creator) {
    return Optional.ofNullable(creator).map(DbUser::getUsername).orElse(null);
  }

  @Named("toTimestampCurrentIfNull")
  public Timestamp timestampCurrentIfNull(Long timestamp) {
    return Optional.ofNullable(timestamp)
        .map(Timestamp::new)
        .orElseGet(() -> Timestamp.from(clock.instant()));
  }

  public String cdrVersionToId(DbCdrVersion cdrVersion) {
    return Optional.ofNullable(cdrVersion)
        .map(DbCdrVersion::getCdrVersionId)
        .map(id -> Long.toString(id))
        .orElse(null);
  }

  @Named("cdrVersionToEtag")
  public String cdrVersionToEtag(int cdrVersion) {
    return Etags.fromVersion(cdrVersion);
  }

  @Named("etagToCdrVersion")
  public int etagToCdrVersion(String etag) {
    return Strings.isNullOrEmpty(etag) ? 1 : Etags.toVersion(etag);
  }

  public BillingStatus checkBillingFeatureFlag(BillingStatus billingStatus) {
    if (!workbenchConfigProvider.get().featureFlags.enableBillingLockout) {
      return BillingStatus.ACTIVE;
    } else {
      return billingStatus;
    }
  }
}
