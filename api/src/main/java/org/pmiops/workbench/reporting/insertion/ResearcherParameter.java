package org.pmiops.workbench.reporting.insertion;

import com.google.cloud.bigquery.QueryParameterValue;
import java.util.function.Function;
import org.pmiops.workbench.cohortbuilder.util.QueryParameterValues.DowncastObject;
import org.pmiops.workbench.model.ReportingResearcher;

public enum ResearcherParameter implements QueryParameterColumn<ReportingResearcher> {
  RESEARCHER_ID("researcher_id", ReportingResearcher::getResearcherId, DowncastObject.INT64),
  USERNAME("username", ReportingResearcher::getUsername, DowncastObject.STRING),
  FIRST_NAME("first_name", ReportingResearcher::getFirstName, DowncastObject.STRING),
  IS_DISABLED("is_disabled", ReportingResearcher::getIsDisabled, DowncastObject.BOOLEAN);

  private final String parameterName;
  private final Function<ReportingResearcher, Object> objectValueFunction;
  private final Function<Object, QueryParameterValue> parameterValueFunction;

  ResearcherParameter(
      String parameterName,
      Function<ReportingResearcher, Object> objectValueFunction,
      Function<Object, QueryParameterValue> parameterValueFunction) {
    this.parameterName = parameterName;
    this.objectValueFunction = objectValueFunction;
    this.parameterValueFunction = parameterValueFunction;
  }

  @Override
  public String getParameterName() {
    return parameterName;
  }

  @Override
  public Function<ReportingResearcher, Object> getObjectValueFunction() {
    return objectValueFunction;
  }

  @Override
  public Function<Object, QueryParameterValue> getQueryParameterValueFunction() {
    return parameterValueFunction;
  }
}
