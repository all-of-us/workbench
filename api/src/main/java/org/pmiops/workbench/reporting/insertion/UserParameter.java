package org.pmiops.workbench.reporting.insertion;

import com.google.cloud.bigquery.QueryParameterValue;
import java.util.function.Function;
import org.pmiops.workbench.model.BqDtoUser;
import org.pmiops.workbench.model.ReportingUser;

public enum UserParameter implements QueryParameterColumn<BqDtoUser> {
  RESEARCHER_ID(
      "researcher_id",
      ReportingUser::getUserId,
      r -> QueryParameterValue.int64(r.getUserId())),
  USERNAME(
      "username",
      ReportingUser::getEmail,
      r -> QueryParameterValue.string(r.getEmail())),
  FIRST_NAME(
      "first_name",
      ReportingUser::getGivenName,
      r -> QueryParameterValue.string(r.getGivenName())),
  IS_DISABLED(
      "is_disabled",
      ReportingUser::getDisabled,
      r -> QueryParameterValue.bool(r.getDisabled()));

  private final String parameterName;
  private final Function<ReportingUser, Object> objectValueFunction;
  private final Function<ReportingUser, QueryParameterValue> parameterValueFunction;

  UserParameter(
      String parameterName,
      Function<ReportingUser, Object> objectValueFunction,
      Function<ReportingUser, QueryParameterValue> parameterValueFunction) {
    this.parameterName = parameterName;
    this.objectValueFunction = objectValueFunction;
    this.parameterValueFunction = parameterValueFunction;
  }

  @Override
  public String getParameterName() {
    return parameterName;
  }

  @Override
  public Function<ReportingUser, Object> getRowToInsertValueFunction() {
    return objectValueFunction;
  }

  @Override
  public Function<ReportingUser, QueryParameterValue> getQueryParameterValueFunction() {
    return parameterValueFunction;
  }
}
