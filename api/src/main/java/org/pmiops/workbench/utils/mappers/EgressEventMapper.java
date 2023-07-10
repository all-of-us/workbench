package org.pmiops.workbench.utils.mappers;

import com.google.gson.Gson;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.pmiops.workbench.db.model.DbEgressEvent;
import org.pmiops.workbench.db.model.DbEgressEvent.DbEgressEventStatus;
import org.pmiops.workbench.model.BucketAuditEntry;
import org.pmiops.workbench.model.EgressEvent;
import org.pmiops.workbench.model.EgressEventStatus;
import org.pmiops.workbench.model.SumologicEgressEvent;

@Mapper(config = MapStructConfig.class, uses = CommonMappers.class)
public interface EgressEventMapper {
  // these are calculated in addWindowTiming()
  @Mapping(target = "timeWindowStartEpochMillis", ignore = true)
  @Mapping(target = "timeWindowEndEpochMillis", ignore = true)
  @Mapping(target = "sourceUserEmail", source = "user.username")
  @Mapping(target = "sourceWorkspaceNamespace", source = "workspace.workspaceNamespace")
  @Mapping(target = "sourceGoogleProject", source = "workspace.googleProject")
  @Mapping(target = "creationTime", qualifiedByName = "timestampToIso8601String")
  EgressEvent toApiEvent(DbEgressEvent event);

  EgressEventStatus toApiStatus(DbEgressEventStatus status);

  @AfterMapping
  default void addWindowTiming(DbEgressEvent source, @MappingTarget EgressEvent target) {
    SumologicEgressEvent sumoEvent = toSumoLogicEvent(source);
    if (sumoEvent == null) {
      return;
    }
    if (sumoEvent.getTimeWindowStart() != null) {
      long timeWindowStart = sumoEvent.getTimeWindowStart();
      long timeWindowMillis = sumoEvent.getTimeWindowDuration() * 1000;
      long timeWindowEnd = timeWindowStart + timeWindowMillis;

      target.timeWindowStartEpochMillis(timeWindowStart).timeWindowEndEpochMillis(timeWindowEnd);
    }
  }

  DbEgressEventStatus toDbStatus(EgressEventStatus status);

  default SumologicEgressEvent toSumoLogicEvent(DbEgressEvent dbEgressEvent) {
    return new Gson().fromJson(dbEgressEvent.getSumologicEvent(), SumologicEgressEvent.class);
  }

  default BucketAuditEntry toBucketAuditEntry(DbEgressEvent dbEgressEvent) {
    if (dbEgressEvent.getBucketAuditEvent() == null) {
      return null;
    }
    return new Gson().fromJson(dbEgressEvent.getBucketAuditEvent(), BucketAuditEntry.class);
  }
}
