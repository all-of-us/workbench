package org.pmiops.workbench.dataset;

import java.util.List;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.pmiops.workbench.cohorts.CohortService;
import org.pmiops.workbench.conceptset.ConceptSetService;
import org.pmiops.workbench.db.model.DbDataDictionaryEntry;
import org.pmiops.workbench.db.model.DbDataset;
import org.pmiops.workbench.db.model.DbDatasetValue;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.model.DataDictionaryEntry;
import org.pmiops.workbench.model.DataSet;
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

  @Mapping(target = "id", source = "dataSetId")
  @Mapping(target = "conceptSets", source = "conceptSetIds")
  @Mapping(target = "cohorts", source = "cohortIds")
  @Mapping(target = "domainValuePairs", source = "values")
  @Mapping(target = "etag", source = "version", qualifiedByName = "cdrVersionToEtag")
  DataSet dbModelToClient(DbDataset dbDataset);

  default PrePackagedConceptSetEnum prePackagedConceptSetFromStorage(Short prePackagedConceptSet) {
    return DbStorageEnums.prePackagedConceptSetsFromStorage(prePackagedConceptSet);
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
