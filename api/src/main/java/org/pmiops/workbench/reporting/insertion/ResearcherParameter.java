package org.pmiops.workbench.reporting.insertion;

import com.google.cloud.bigquery.QueryParameterValue;
import java.util.function.Function;
import org.pmiops.workbench.model.ReportingResearcher;

public enum ResearcherParameter implements QueryParameterColumn<ReportingResearcher> {
  RESEARCHER_ID(
      "researcher_id",
      ReportingResearcher::getResearcherId,
      r -> QueryParameterValue.int64(r.getResearcherId())),
  USERNAME(
      "username",
      ReportingResearcher::getUsername,
      r -> QueryParameterValue.string(r.getUsername())),
  FIRST_NAME(
      "first_name",
      ReportingResearcher::getFirstName,
      r -> QueryParameterValue.string(r.getFirstName())),
  IS_DISABLED(
      "is_disabled",
      ReportingResearcher::getIsDisabled,
      r -> QueryParameterValue.bool(r.getIsDisabled()));

  private final String parameterName;
  private final Function<ReportingResearcher, Object> objectValueFunction;
  private final Function<ReportingResearcher, QueryParameterValue> parameterValueFunction;

  ResearcherParameter(
      String parameterName,
      Function<ReportingResearcher, Object> objectValueFunction,
      Function<ReportingResearcher, QueryParameterValue> parameterValueFunction) {
    this.parameterName = parameterName;
    this.objectValueFunction = objectValueFunction;
    this.parameterValueFunction = parameterValueFunction;
  }

  @Override
  public String getParameterName() {
    return parameterName;
  }

  @Override
  public Function<ReportingResearcher, Object> getRowToInsertValueFunction() {
    return objectValueFunction;
  }

  @Override
  public Function<ReportingResearcher, QueryParameterValue> getQueryParameterValueFunction() {
    return parameterValueFunction;
  }
}
