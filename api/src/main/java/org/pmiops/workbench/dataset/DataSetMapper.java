package org.pmiops.workbench.dataset;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.pmiops.workbench.db.model.DbDataDictionaryEntry;
import org.pmiops.workbench.model.DataDictionaryEntry;
import org.pmiops.workbench.utils.mappers.CommonMappers;

@Mapper(
    componentModel = "spring",
    uses = {CommonMappers.class})
public interface DataSetMapper {

  @Mapping(target = "cdrVersionId", source = "dbModel.cdrVersion.cdrVersionId")
  DataDictionaryEntry toApi(DbDataDictionaryEntry dbModel);
}
