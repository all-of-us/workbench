package org.pmiops.workbench.reporting.insertion;

import static org.pmiops.workbench.cohortbuilder.util.QueryParameterValues.enumToString;
import static org.pmiops.workbench.cohortbuilder.util.QueryParameterValues.toInsertRowString;

import java.util.function.Function;
import org.pmiops.workbench.model.ReportingNewUserSatisfactionSurvey;

/*
 * Column data and metadata convertors for BigQuery new_user_satisfaction_survey table in reporting
 * dataset.
 */
public enum NewUserSatisfactionSurveyColumnValueExtractor
    implements ColumnValueExtractor<ReportingNewUserSatisfactionSurvey> {
  ID("id", ReportingNewUserSatisfactionSurvey::getId),
  USER_ID("user_id", ReportingNewUserSatisfactionSurvey::getUserId),
  CREATED(
      "created",
      newUserSatisfactionSurvey -> toInsertRowString(newUserSatisfactionSurvey.getCreated())),
  MODIFIED(
      "modified",
      newUserSatisfactionSurvey -> toInsertRowString(newUserSatisfactionSurvey.getModified())),
  SATISFACTION(
      "satisfaction",
      newUserSatisfactionSurvey -> enumToString(newUserSatisfactionSurvey.getSatisfaction())),
  ADDITIONAL_INFO("additional_info", ReportingNewUserSatisfactionSurvey::getAdditionalInfo);

  public static final String TABLE_NAME = "new_user_satisfaction_survey";

  private final String parameterName;
  private final Function<ReportingNewUserSatisfactionSurvey, Object> rowToInsertValueFunction;

  NewUserSatisfactionSurveyColumnValueExtractor(
      String parameterName,
      Function<ReportingNewUserSatisfactionSurvey, Object> rowToInsertValueFunction) {
    this.parameterName = parameterName;
    this.rowToInsertValueFunction = rowToInsertValueFunction;
  }

  @Override
  public String getParameterName() {
    return parameterName;
  }

  @Override
  public Function<ReportingNewUserSatisfactionSurvey, Object> getRowToInsertValueFunction() {
    return rowToInsertValueFunction;
  }
}
