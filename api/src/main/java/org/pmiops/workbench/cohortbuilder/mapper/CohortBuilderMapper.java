package org.pmiops.workbench.cohortbuilder.mapper;

import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.pmiops.workbench.cdr.model.DbAgeTypeCount;
import org.pmiops.workbench.cdr.model.DbCBMenu;
import org.pmiops.workbench.cdr.model.DbCriteria;
import org.pmiops.workbench.cdr.model.DbCriteriaAttribute;
import org.pmiops.workbench.cdr.model.DbCriteriaMenu;
import org.pmiops.workbench.cdr.model.DbDataFilter;
import org.pmiops.workbench.cdr.model.DbDomainInfo;
import org.pmiops.workbench.cdr.model.DbSurveyModule;
import org.pmiops.workbench.cdr.model.DbSurveyVersion;
import org.pmiops.workbench.model.AgeTypeCount;
import org.pmiops.workbench.model.Criteria;
import org.pmiops.workbench.model.CriteriaAttribute;
import org.pmiops.workbench.model.CriteriaMenu;
import org.pmiops.workbench.model.DataFilter;
import org.pmiops.workbench.model.DomainInfo;
import org.pmiops.workbench.model.SurveyModule;
import org.pmiops.workbench.model.SurveyVersion;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.utils.mappers.MapStructConfig;

@Mapper(
    config = MapStructConfig.class,
    uses = {CommonMappers.class})
public interface CohortBuilderMapper {

  AgeTypeCount dbModelToClient(DbAgeTypeCount source);

  @Mapping(target = "conceptId", source = "longConceptId")
  @Mapping(target = "hasAttributes", source = "attribute")
  @Mapping(target = "hasAncestorData", source = "ancestorData")
  @Mapping(target = "hasHierarchy", source = "hierarchy")
  @Mapping(target = "isStandard", source = "standard")
  Criteria dbModelToClient(DbCriteria source);

  @AfterMapping
  default void afterMappingDbModelToClient(
      @MappingTarget Criteria criteria, @Context Boolean isStandard, @Context Long childCount) {
    criteria.setIsStandard(isStandard);
    criteria.setChildCount(childCount);
    criteria.setParentCount(0L);
    criteria.setSelectable(true);
  }

  CriteriaAttribute dbModelToClient(DbCriteriaAttribute source);

  DataFilter dbModelToClient(DbDataFilter source);

  SurveyVersion dbModelToClient(DbSurveyVersion source);

  @Mapping(target = "domain", source = "domainEnum")
  DomainInfo dbModelToClient(DbDomainInfo source);

  SurveyModule dbModelToClient(DbSurveyModule source);

  @Mapping(target = "standard", ignore = true)
  CriteriaMenu dbModelToClient(DbCriteriaMenu source);

  CriteriaMenu dbModelToClient(DbCBMenu source);
}
