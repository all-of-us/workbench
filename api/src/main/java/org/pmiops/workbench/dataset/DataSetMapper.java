package org.pmiops.workbench.dataset;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.pmiops.workbench.db.model.DbDataDictionaryEntry;
import org.pmiops.workbench.db.model.DbDataset;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.model.DataDictionaryEntry;
import org.pmiops.workbench.model.DataSet;
import org.pmiops.workbench.model.PrePackagedConceptSetEnum;
import org.pmiops.workbench.utils.mappers.CommonMappers;

@Mapper(
    componentModel = "spring",
    uses = {CommonMappers.class})
public interface DataSetMapper {

  @Mapping(target = "id", source = "dataSetId")
  @Mapping(target = "conceptSets", ignore = true) // This is stored as a list of ids. Look those up in the controller for entities
  @Mapping(target = "cohorts", ignore = true) // This is stored as a list of ids. Look those up in the controller for entities
  @Mapping(target = "domainValuePairs", ignore = true) // This is stored in a subtable, we may not want to fetch all the time
  @Mapping(target = "etag", source = "version")
  DataSet dbModelToClient(DbDataset dbDataset);

  default PrePackagedConceptSetEnum prePackagedConceptSetFromStorage(Short prePackagedConceptSet) {
    return DbStorageEnums.prePackagedConceptSetsFromStorage(prePackagedConceptSet);
  }

  @Mapping(target = "cdrVersionId", source = "dbModel.cdrVersion.cdrVersionId")
  DataDictionaryEntry toApi(DbDataDictionaryEntry dbModel);
}
