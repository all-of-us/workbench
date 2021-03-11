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
import org.pmiops.workbench.cdr.model.DbDSDataDictionary;
import org.pmiops.workbench.cohorts.CohortService;
import org.pmiops.workbench.conceptset.ConceptSetService;
import org.pmiops.workbench.db.model.DbDataDictionaryEntry;
import org.pmiops.workbench.db.model.DbDataset;
import org.pmiops.workbench.db.model.DbDatasetValue;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.model.DataDictionaryEntry;
import org.pmiops.workbench.model.DataSet;
import org.pmiops.workbench.model.DataSetRequest;
import org.pmiops.workbench.model.DomainValuePair;
import org.pmiops.workbench.model.PrePackagedConceptSetEnum;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.utils.mappers.MapStructConfig;

@Mapper(
    config = MapStructConfig.class,
    uses = {CommonMappers.class, ConceptSetService.class, CohortService.class})
public interface DataSetMapper {

  // This is a lightweight version of a client mapper that doesn't make any extra db calls for extra
  // data
  @Mapping(target = "id", source = "dataSetId")
  // This is stored as a list of ids. Look those up in the controller for entities
  @Mapping(target = "conceptSets", ignore = true)
  // This is stored as a list of ids. Look those up in the controller for entities
  @Mapping(target = "cohorts", ignore = true)
  // This is stored in a subtable, we may not want to fetch all the time
  @Mapping(target = "domainValuePairs", ignore = true)
  @Mapping(target = "etag", source = "version", qualifiedByName = "cdrVersionToEtag")
  // TODO (RW-4756): Define a DatasetLight type.
  @Named("dbModelToClientLight")
  DataSet dbModelToClientLight(DbDataset dbDataset);

  @Mapping(target = "dataSetId", ignore = true)
  @Mapping(
      target = "version",
      source = "etag",
      qualifiedByName = "etagToCdrVersion",
      nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
  @Mapping(target = "creatorId", ignore = true)
  @Mapping(target = "creationTime", ignore = true)
  @Mapping(target = "invalid", ignore = true)
  @Mapping(target = "lastModifiedTime", ignore = true)
  @Mapping(target = "values", source = "domainValuePairs")
  @Mapping(target = "prePackagedConceptSetEnum", ignore = true)
  DbDataset dataSetRequestToDb(
      DataSetRequest dataSetRequest, @Context DbDataset dbDataSet, @Context Clock clock);

  @Mapping(target = "id", source = "dataSetId")
  @Mapping(target = "conceptSets", source = "conceptSetIds")
  @Mapping(target = "cohorts", source = "cohortIds")
  @Mapping(target = "domainValuePairs", source = "values")
  @Mapping(target = "etag", source = "version", qualifiedByName = "cdrVersionToEtag")
  DataSet dbModelToClient(DbDataset dbDataset);

  @AfterMapping
  default void populateFromSourceDbObject(
      @MappingTarget DbDataset targetDb, @Context DbDataset dbDataSet, @Context Clock clock) {
    targetDb.setInvalid(dbDataSet == null ? false : dbDataSet.getInvalid());
    Timestamp now = new Timestamp(clock.instant().toEpochMilli());
    if (dbDataSet == null) {
      targetDb.setInvalid(false);
      targetDb.setCreationTime(now);
      targetDb.setLastModifiedTime(now);
    } else {
      targetDb.setCreationTime(dbDataSet.getCreationTime());
      targetDb.setDataSetId(dbDataSet.getDataSetId());
      targetDb.setCreatorId(dbDataSet.getCreatorId());
      targetDb.setLastModifiedTime(now);
    }
    if (targetDb.getValues().isEmpty()) {
      // In case of rename, dataSetRequest does not have cohort/Concept ID information
      targetDb.setConceptSetIds(dbDataSet.getConceptSetIds());
      targetDb.setCohortIds(dbDataSet.getCohortIds());
      targetDb.setValues(dbDataSet.getValues());
      targetDb.setIncludesAllParticipants(dbDataSet.getIncludesAllParticipants());
      targetDb.setPrePackagedConceptSet(dbDataSet.getPrePackagedConceptSet());
    }
  }

  default List<PrePackagedConceptSetEnum> prePackagedConceptSetFromStorage(
      List<Short> prePackagedConceptSet) {
    return prePackagedConceptSet.stream()
        .map(DbStorageEnums::prePackagedConceptSetsFromStorage)
        .collect(Collectors.toList());
  }

  default List<Short> toDBPrePackagedConceptSet(
      List<PrePackagedConceptSetEnum> prePackagedConceptSetEnum) {
    return prePackagedConceptSetEnum.stream()
        .map(DbStorageEnums::prePackagedConceptSetsToStorage)
        .collect(Collectors.toList());
  }

  default List<DbDatasetValue> toDbDomainValuePairs(List<DomainValuePair> domainValuePairs) {
    if (domainValuePairs != null) {
      return domainValuePairs.stream()
          .map(this::getDataSetValuesFromDomainValueSet)
          .collect(toImmutableList());
    }
    return new ArrayList<DbDatasetValue>();
  }

  default DbDatasetValue getDataSetValuesFromDomainValueSet(DomainValuePair domainValuePair) {
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


//  transformedByRegisteredTierPrivacyMethods and definedTime are not used in UI we can delete this later
  @Mapping(target = "cdrVersionId", ignore = true)
  @Mapping(target = "transformedByRegisteredTierPrivacyMethods", ignore = true)
  @Mapping(target = "definedTime", ignore = true)
  DataDictionaryEntry dbDsModelToClient(DbDSDataDictionary dbModel);
}
