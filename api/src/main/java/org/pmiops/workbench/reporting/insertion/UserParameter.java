package org.pmiops.workbench.reporting.insertion;

import com.google.cloud.bigquery.QueryParameterValue;
import java.util.function.Function;
import org.pmiops.workbench.model.BqDtoUser;
import org.pmiops.workbench.model.BqDtoUser;

public enum UserParameter implements QueryParameterColumn<BqDtoUser> {
  RESEARCHER_ID(
      "researcher_id",
      BqDtoUser::getUserId,
      r -> QueryParameterValue.int64(r.getUserId())),
  USERNAME(
      "username",
      BqDtoUser::getUsername,
      r -> QueryParameterValue.string(r.getUsername())),
  FIRST_NAME(
      "first_name",
      BqDtoUser::getGivenName,
      r -> QueryParameterValue.string(r.getGivenName())),
  IS_DISABLED(
      "is_disabled",
      BqDtoUser::getDisabled,
      r -> QueryParameterValue.bool(r.getDisabled()));

  private final String parameterName;
  private final Function<BqDtoUser, Object> objectValueFunction;
  private final Function<BqDtoUser, QueryParameterValue> parameterValueFunction;

  UserParameter(
      String parameterName,
      Function<BqDtoUser, Object> objectValueFunction,
      Function<BqDtoUser, QueryParameterValue> parameterValueFunction) {
    this.parameterName = parameterName;
    this.objectValueFunction = objectValueFunction;
    this.parameterValueFunction = parameterValueFunction;
  }

  @Override
  public String getParameterName() {
    return parameterName;
  }

  @Override
  public Function<BqDtoUser, Object> getRowToInsertValueFunction() {
    return objectValueFunction;
  }

  @Override
  public Function<BqDtoUser, QueryParameterValue> getQueryParameterValueFunction() {
    return parameterValueFunction;
  }
}
