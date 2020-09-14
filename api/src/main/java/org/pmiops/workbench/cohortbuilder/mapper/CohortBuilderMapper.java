package org.pmiops.workbench.cohortbuilder.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.pmiops.workbench.cdr.model.DbAgeTypeCount;
import org.pmiops.workbench.cdr.model.DbCriteria;
import org.pmiops.workbench.cdr.model.DbCriteriaAttribute;
import org.pmiops.workbench.cdr.model.DbDataFilter;
import org.pmiops.workbench.model.AgeTypeCount;
import org.pmiops.workbench.model.Criteria;
import org.pmiops.workbench.model.CriteriaAttribute;
import org.pmiops.workbench.model.DataFilter;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.utils.mappers.MapStructConfig;

@Mapper(
    config = MapStructConfig.class,
    uses = {CommonMappers.class})
public interface CohortBuilderMapper {

  AgeTypeCount dbModelToClient(DbAgeTypeCount source);

  @Mapping(target = "count", source = "longCount")
  @Mapping(target = "parentCount", source = "longParentCount")
  @Mapping(target = "childCount", source = "longChildCount")
  @Mapping(target = "conceptId", source = "longConceptId")
  @Mapping(target = "hasAttributes", source = "attribute")
  @Mapping(target = "hasAncestorData", source = "ancestorData")
  @Mapping(target = "hasHierarchy", source = "hierarchy")
  @Mapping(target = "isStandard", source = "standard")
  Criteria dbModelToClient(DbCriteria source);

  CriteriaAttribute dbModelToClient(DbCriteriaAttribute source);

  DataFilter dbModelToClient(DbDataFilter source);
}
