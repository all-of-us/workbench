package org.pmiops.workbench.dataset.mapper;

import static com.google.common.collect.ImmutableList.toImmutableList;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValueCheckStrategy;
import org.pmiops.workbench.cohorts.CohortService;
import org.pmiops.workbench.conceptset.ConceptSetService;
import org.pmiops.workbench.db.model.DbDataDictionaryEntry;
import org.pmiops.workbench.db.model.DbDataset;
import org.pmiops.workbench.db.model.DbDatasetValue;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.model.DataDictionaryEntry;
import org.pmiops.workbench.model.Dataset;
import org.pmiops.workbench.model.DatasetRequest;
import org.pmiops.workbench.model.DomainValuePair;
import org.pmiops.workbench.model.PrePackagedConceptSetEnum;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.utils.mappers.MapStructConfig;

@Mapper(
    config = MapStructConfig.class,
    uses = {CommonMappers.class, ConceptSetService.class, CohortService.class})
public interface DatasetMapper {

  // This is a lightweight version of a client mapper that doesn't make any extra db calls for extra
  // data
  @Mapping(target = "id", source = "datasetId")
  // This is stored as a list of ids. Look those up in the controller for entities
  @Mapping(target = "conceptSets", ignore = true)
  // This is stored as a list of ids. Look those up in the controller for entities
  @Mapping(target = "cohorts", ignore = true)
  // This is stored in a subtable, we may not want to fetch all the time
  @Mapping(target = "domainValuePairs", ignore = true)
  @Mapping(target = "etag", source = "version", qualifiedByName = "cdrVersionToEtag")
  // TODO (RW-4756): Define a DatasetLight type.
  @Named("dbModelToClientLight")
  Dataset dbModelToClientLight(DbDataset dbDataset);

  @Mapping(target = "datasetId", ignore = true)
  @Mapping(
      target = "version",
      source = "etag",
      qualifiedByName = "etagToCdrVersion",
      nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
  @Mapping(target = "creatorId", ignore = true)
  @Mapping(target = "creationTime", ignore = true)
  @Mapping(target = "invalid", ignore = true)
  @Mapping(target = "lastModifiedTime", ignore = true)
  @Mapping(target = "prePackagedConceptSetEnum", ignore = true)
  @Mapping(target = "values", source = "domainValuePairs")
  DbDataset datasetRequestToDb(
      DatasetRequest datasetRequest, @Context DbDataset dbDataset, @Context Clock clock);

  @Mapping(target = "id", source = "datasetId")
  @Mapping(target = "conceptSets", source = "conceptSetIds")
  @Mapping(target = "cohorts", source = "cohortIds")
  @Mapping(target = "domainValuePairs", source = "values")
  @Mapping(target = "etag", source = "version", qualifiedByName = "cdrVersionToEtag")
  Dataset dbModelToClient(DbDataset dbDataset);

  @AfterMapping
  default void populateFromSourceDbObject(
      @MappingTarget DbDataset targetDb, @Context DbDataset dbDataset, @Context Clock clock) {
    targetDb.setInvalid(dbDataset == null ? false : dbDataset.getInvalid());
    Timestamp now = new Timestamp(clock.instant().toEpochMilli());
    if (dbDataset == null) {
      targetDb.setInvalid(false);
      targetDb.setCreationTime(now);
      targetDb.setLastModifiedTime(now);
    } else {
      targetDb.setCreationTime(dbDataset.getCreationTime());
      targetDb.setDatasetId(dbDataset.getDatasetId());
      targetDb.setCreatorId(dbDataset.getCreatorId());
      targetDb.setLastModifiedTime(now);
    }
    if (targetDb.getValues().isEmpty()) {
      // In case of rename, datasetRequest does not have cohort/Concept ID information
      targetDb.setConceptSetIds(dbDataset.getConceptSetIds());
      targetDb.setCohortIds(dbDataset.getCohortIds());
      targetDb.setValues(dbDataset.getValues());
      targetDb.setIncludesAllParticipants(dbDataset.getIncludesAllParticipants());
      targetDb.setPrePackagedConceptSet(dbDataset.getPrePackagedConceptSet());
    }
  }

  default PrePackagedConceptSetEnum prePackagedConceptSetFromStorage(Short prePackagedConceptSet) {
    return DbStorageEnums.prePackagedConceptSetsFromStorage(prePackagedConceptSet);
  }

  default Short toDBPrePackagedConceptSet(PrePackagedConceptSetEnum prePackagedConceptSetEnum) {
    return DbStorageEnums.prePackagedConceptSetsToStorage(prePackagedConceptSetEnum);
  }

  default List<DbDatasetValue> toDbDomainValuePairs(List<DomainValuePair> domainValuePairs) {
    if (domainValuePairs != null) {
      return domainValuePairs.stream()
          .map(this::getDatasetValuesFromDomainValueSet)
          .collect(toImmutableList());
    }
    return new ArrayList<DbDatasetValue>();
  }

  default DbDatasetValue getDatasetValuesFromDomainValueSet(DomainValuePair domainValuePair) {
    return new DbDatasetValue(
        DbStorageEnums.domainToStorage(domainValuePair.getDomain()).toString(),
        domainValuePair.getValue());
  }

  default List<DomainValuePair> copyDomainValuePairsToClient(List<DbDatasetValue> values) {
    return values.stream().map(this::createDomainValuePair).collect(Collectors.toList());
  }

  default DomainValuePair createDomainValuePair(DbDatasetValue dbDatasetValue) {
    return new DomainValuePair()
        .value(dbDatasetValue.getValue())
        .domain(dbDatasetValue.getDomainEnum());
  }

  @Mapping(target = "cdrVersionId", source = "dbModel.cdrVersion.cdrVersionId")
  DataDictionaryEntry dbModelToClient(DbDataDictionaryEntry dbModel);
}
