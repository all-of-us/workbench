package org.pmiops.workbench.cohortbuilder.mappers;

import org.mapstruct.Mapper;
import org.pmiops.workbench.cdr.model.DbCriteriaAttribute;
import org.pmiops.workbench.model.CriteriaAttribute;
import org.pmiops.workbench.utils.mappers.CommonMappers;

@Mapper(
    componentModel = "spring",
    uses = {CommonMappers.class})
public interface CriteriaAttributeMapper {

  CriteriaAttribute dbModelToClient(DbCriteriaAttribute source);
}
