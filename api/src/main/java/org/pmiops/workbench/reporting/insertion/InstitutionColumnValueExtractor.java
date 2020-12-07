package org.pmiops.workbench.reporting.insertion;

import static com.google.cloud.bigquery.QueryParameterValue.int64;
import static com.google.cloud.bigquery.QueryParameterValue.string;
import static org.pmiops.workbench.cohortbuilder.util.QueryParameterValues.enumToQpv;
import static org.pmiops.workbench.cohortbuilder.util.QueryParameterValues.enumToString;

import com.google.cloud.bigquery.QueryParameterValue;
import java.util.function.Function;
import org.pmiops.workbench.model.ReportingInstitution;

public enum InstitutionColumnValueExtractor implements ColumnValueExtractor<ReportingInstitution> {
  DISPLAY_NAME(
      "display_name", ReportingInstitution::getDisplayName),
  DUA_TYPE_ENUM(
      "dua_type_enum", i -> enumToString(i.getDuaTypeEnum())),
  INSTITUTION_ID(
      "institution_id", ReportingInstitution::getInstitutionId),
  ORGANIZATION_TYPE_ENUM(
      "organization_type_enum",
      i -> enumToString(i.getOrganizationTypeEnum())),
  ORGANIZATION_TYPE_OTHER_TEXT(
      "organization_type_other_text",
      ReportingInstitution::getOrganizationTypeOtherText),
  SHORT_NAME("short_name", ReportingInstitution::getShortName);

  // Much of the repetitive boilerplate below (constructor, setters, etc) can't really be helped,
  // as enums can't be abstract or extend abstract classes.
  private static final String TABLE_NAME = "institution";
  private final String parameterName;
  private final Function<ReportingInstitution, Object> objectValueFunction;

  InstitutionColumnValueExtractor(
      String parameterName,
      Function<ReportingInstitution, Object> objectValueFunction) {
    this.parameterName = parameterName;
    this.objectValueFunction = objectValueFunction;
  }

  @Override
  public String getBigQueryTableName() {
    return TABLE_NAME;
  }

  @Override
  public String getParameterName() {
    return parameterName;
  }

  @Override
  public Function<ReportingInstitution, Object> getRowToInsertValueFunction() {
    return objectValueFunction;
  }
}
