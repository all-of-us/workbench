package org.pmiops.workbench.cohortbuilder.mappers;

import org.mapstruct.Mapper;
import org.pmiops.workbench.cdr.model.DbAgeTypeCount;
import org.pmiops.workbench.model.AgeTypeCount;
import org.pmiops.workbench.utils.mappers.CommonMappers;

@Mapper(
    componentModel = "spring",
    uses = {CommonMappers.class})
public interface AgeTypeCountMapper {

  AgeTypeCount dbModelToClient(DbAgeTypeCount source);
}
