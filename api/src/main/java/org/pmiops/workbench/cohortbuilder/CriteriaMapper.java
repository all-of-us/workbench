package org.pmiops.workbench.cohortbuilder;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.pmiops.workbench.cdr.model.DbCriteria;
import org.pmiops.workbench.model.Criteria;
import org.pmiops.workbench.utils.mappers.CommonMappers;

@Mapper(
    componentModel = "spring",
    uses = {CommonMappers.class})
public interface CriteriaMapper {

  @Mapping(target = "count", source = "longCount")
  @Mapping(target = "conceptId", source = "longConceptId")
  @Mapping(target = "hasAttributes", source = "attribute")
  @Mapping(target = "hasAncestorData", source = "ancestorData")
  @Mapping(target = "hasHierarchy", source = "hierarchy")
  @Mapping(target = "isStandard", source = "standard")
  Criteria dbModelToClient(DbCriteria source);
}
