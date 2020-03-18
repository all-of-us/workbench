package org.pmiops.workbench.cohortbuilder.mappers;

import org.mapstruct.Mapper;
import org.pmiops.workbench.cdr.model.DbDataFilter;
import org.pmiops.workbench.model.DataFilter;
import org.pmiops.workbench.utils.mappers.CommonMappers;

@Mapper(
    componentModel = "spring",
    uses = {CommonMappers.class})
public interface DataFilterMapper {

  DataFilter dbModelToClient(DbDataFilter source);
}
