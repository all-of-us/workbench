package org.pmiops.workbench.reporting.insertion;

import static org.pmiops.workbench.cohortbuilder.util.QueryParameterValues.toInsertRowString;

import java.util.function.Function;
import org.pmiops.workbench.model.ReportingDemographicSurveyV2;

/*
 * Column data and metadata convertors for BigQuery demographic_survey_v2 table in reporting
 * dataset.
 */
public enum DemographicSurveyV2ColumnValueExtractor
    implements ColumnValueExtractor<ReportingDemographicSurveyV2> {
  DEMOGRAPHIC_SURVEY_V2_ID("id", ReportingDemographicSurveyV2::getDemographicSurveyV2Id),
  USER_ID("user_id", ReportingDemographicSurveyV2::getUserId),
  COMPLETION_TIME("completion_time", demographicSurveyV2 -> toInsertRowString(demographicSurveyV2.getCompletionTime())),
  ETHNICITY_AI_AN_OTHER_TEXT(
      "ethnicity_ai_an_other_text", ReportingDemographicSurveyV2::getEthnicityAiAnOtherText),
  ETHNICITY_ASIAN_OTHER_TEXT(
      "ethnicity_asian_other_text", ReportingDemographicSurveyV2::getEthnicityAsianOtherText),
  ETHNICITY_BLACK_OTHER_TEXT(
      "ethnicity_black_other_text", ReportingDemographicSurveyV2::getEthnicityBlackOtherText),
  ETHNICITY_HISPANIC_OTHER_TEXT(
      "ethnicity_hispanic_other_text", ReportingDemographicSurveyV2::getEthnicityHispanicOtherText),
  ETHNICITY_ME_NA_OTHER_TEXT(
      "ethnicity_me_na_other_text", ReportingDemographicSurveyV2::getEthnicityMeNaOtherText),
  ETHNICITY_NH_PI_OTHER_TEXT(
      "ethnicity_nh_pi_other_text", ReportingDemographicSurveyV2::getEthnicityNhPiOtherText),
  ETHNICITY_WHITE_OTHER_TEXT(
      "ethnicity_white_other_text", ReportingDemographicSurveyV2::getEthnicityWhiteOtherText),
  ETHNICITY_OTHER_TEXT("ethnicity_other_text", ReportingDemographicSurveyV2::getEthnicityOtherText),
  GENDER_OTHER_TEXT("gender_other_text", ReportingDemographicSurveyV2::getGenderOtherText),
  ORIENTATION_OTHER_TEXT(
      "orientation_other_text", ReportingDemographicSurveyV2::getOrientationOtherText),
  SEX_AT_BIRTH("sex_at_birth", ReportingDemographicSurveyV2::getSexAtBirth),
  SEX_AT_BIRTH_OTHER_TEXT(
      "sex_at_birth_other_text", ReportingDemographicSurveyV2::getSexAtBirthOtherText),
  YEAR_OF_BIRTH("year_of_birth", ReportingDemographicSurveyV2::getYearOfBirth),
  YEAR_OF_BIRTH_PREFER_NOT(
      "year_of_birth_prefer_not", ReportingDemographicSurveyV2::isYearOfBirthPreferNot),
  DISABILITY_HEARING("disability_hearing", ReportingDemographicSurveyV2::getDisabilityHearing),
  DISABILITY_SEEING("disability_seeing", ReportingDemographicSurveyV2::getDisabilitySeeing),
  DISABILITY_CONCENTRATING(
      "disability_concentrating", ReportingDemographicSurveyV2::getDisabilityConcentrating),
  DISABILITY_WALKING("disability_walking", ReportingDemographicSurveyV2::getDisabilityWalking),
  DISABILITY_DRESSING("disability_dressing", ReportingDemographicSurveyV2::getDisabilityDressing),
  DISABILITY_ERRANDS("disability_errands", ReportingDemographicSurveyV2::getDisabilityErrands),
  DISABILITY_OTHER_TEXT(
      "disability_other_text", ReportingDemographicSurveyV2::getDisabilityOtherText),
  EDUCATION("education", ReportingDemographicSurveyV2::getEducation),
  DISADVANTAGED("disadvantaged", ReportingDemographicSurveyV2::getDisadvantaged),
  SURVEY_COMMENTS("survey_comments", ReportingDemographicSurveyV2::getSurveyComments);

  private final String parameterName;
  private final Function<ReportingDemographicSurveyV2, Object> rowToInsertValueFunction;

  DemographicSurveyV2ColumnValueExtractor(
      String parameterName,
      Function<ReportingDemographicSurveyV2, Object> rowToInsertValueFunction) {
    this.parameterName = parameterName;
    this.rowToInsertValueFunction = rowToInsertValueFunction;
  }

  @Override
  public String getParameterName() {
    return parameterName;
  }

  @Override
  public Function<ReportingDemographicSurveyV2, Object> getRowToInsertValueFunction() {
    return rowToInsertValueFunction;
  }
}
