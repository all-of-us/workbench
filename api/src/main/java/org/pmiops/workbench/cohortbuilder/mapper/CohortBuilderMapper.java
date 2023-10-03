package org.pmiops.workbench.cohortbuilder.mapper;

import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.TableResult;
import com.google.common.collect.ImmutableList;
import java.util.stream.StreamSupport;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.pmiops.workbench.cdr.model.DbAgeTypeCount;
import org.pmiops.workbench.cdr.model.DbCardCount;
import org.pmiops.workbench.cdr.model.DbCriteria;
import org.pmiops.workbench.cdr.model.DbCriteriaAttribute;
import org.pmiops.workbench.cdr.model.DbCriteriaMenu;
import org.pmiops.workbench.cdr.model.DbDataFilter;
import org.pmiops.workbench.cdr.model.DbDomainCard;
import org.pmiops.workbench.cdr.model.DbSurveyModule;
import org.pmiops.workbench.cdr.model.DbSurveyVersion;
import org.pmiops.workbench.model.*;
import org.pmiops.workbench.utils.FieldValues;
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
  Criteria dbModelToClient(DbCriteria source);

  CriteriaAttribute dbModelToClient(DbCriteriaAttribute source);

  DataFilter dbModelToClient(DbDataFilter source);

  SurveyVersion dbModelToClient(DbSurveyVersion source);

  @Mapping(target = "domain", source = "domainEnum")
  DomainCard dbModelToClient(DbDomainCard source);

  SurveyModule dbModelToClient(DbSurveyModule source);

  CriteriaMenu dbModelToClient(DbCriteriaMenu source);

  @Mapping(target = "domain", source = "source.domainId", qualifiedByName = "domainIdToDomain")
  CardCount dbModelToClient(DbCardCount source);

  default CohortChartData fieldValueListToCohortChartData(FieldValueList row) {
    CohortChartData cohortChartData = new CohortChartData();
    FieldValues.getString(row, "name").ifPresent(cohortChartData::setName);
    FieldValues.getLong(row, "conceptId").ifPresent(cohortChartData::setConceptId);
    FieldValues.getLong(row, "count").ifPresent(cohortChartData::setCount);
    return cohortChartData;
  }

  default ImmutableList<CohortChartData> tableResultToCohortChartData(TableResult tableResult) {
    return StreamSupport.stream(tableResult.iterateAll().spliterator(), false)
        .map(this::fieldValueListToCohortChartData)
        .collect(ImmutableList.toImmutableList());
  }

  default DemoChartInfo fieldValueListToDemoChartInfo(FieldValueList row) {
    DemoChartInfo demoChartInfo = new DemoChartInfo();
    FieldValues.getString(row, "name").ifPresent(demoChartInfo::setName);
    FieldValues.getString(row, "ageRange").ifPresent(demoChartInfo::setAgeRange);
    FieldValues.getLong(row, "count").ifPresent(demoChartInfo::setCount);
    return demoChartInfo;
  }

  default ImmutableList<DemoChartInfo> tableResultToDemoChartInfo(TableResult tableResult) {
    return StreamSupport.stream(tableResult.iterateAll().spliterator(), false)
        .map(this::fieldValueListToDemoChartInfo)
        .collect(ImmutableList.toImmutableList());
  }

  default EthnicityInfo fieldValueListToEthnicityInfo(FieldValueList row) {
    EthnicityInfo ethnicityInfo = new EthnicityInfo();
    FieldValues.getString(row, "ethnicity").ifPresent(ethnicityInfo::setEthnicity);
    FieldValues.getLong(row, "count").ifPresent(ethnicityInfo::setCount);
    return ethnicityInfo;
  }

  default ImmutableList<EthnicityInfo> tableResultToEthnicityInfo(TableResult tableResult) {
    return StreamSupport.stream(tableResult.iterateAll().spliterator(), false)
        .map(this::fieldValueListToEthnicityInfo)
        .collect(ImmutableList.toImmutableList());
  }
}
