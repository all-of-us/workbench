package org.pmiops.workbench.dataset

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.pmiops.workbench.db.model.DataDictionaryEntry
import org.pmiops.workbench.utils.CommonMappers

@Mapper(componentModel = "spring", uses = [CommonMappers::class])
interface DataSetMapper {

    @Mapping(target = "cdrVersionId", source = "dbModel.cdrVersion.cdrVersionId")
    fun toApi(dbModel: DataDictionaryEntry): org.pmiops.workbench.model.DataDictionaryEntry
}
