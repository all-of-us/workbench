import { DemographicSurveyV2 } from 'generated/fetch';

import {
  NONE_FULLY_DESCRIBE_MAX_LENGTH,
  OTHER_DISABILITY_MAX_LENGTH,
  SURVEY_COMMENTS_MAX_LENGTH,
  validateDemographicSurvey,
  validateDemographicSurvey_V2,
} from './demographic-survey-v2-validation';

describe('DemographicsSurveyV2 validation', () => {
  it('V2 validation matches V1 - blank fields', () => {
    const surveyFields: DemographicSurveyV2 = {};
    const v1 = validateDemographicSurvey(surveyFields);
    const v2 = validateDemographicSurvey_V2(surveyFields);

    expect(v2).toEqual(v1);
  });
  it('V2 validation matches V1 - max lengths', () => {
    const longOtherText = 'a'.repeat(NONE_FULLY_DESCRIBE_MAX_LENGTH + 1);
    const surveyFields: DemographicSurveyV2 = {
      disabilityOtherText: 'd'.repeat(OTHER_DISABILITY_MAX_LENGTH + 1),
      ethnicityAiAnOtherText: longOtherText,
      ethnicityAsianOtherText: longOtherText,
      ethnicityBlackOtherText: longOtherText,
      ethnicityHispanicOtherText: longOtherText,
      ethnicityMeNaOtherText: longOtherText,
      ethnicityNhPiOtherText: longOtherText,
      ethnicityWhiteOtherText: longOtherText,
      ethnicityOtherText: longOtherText,
      genderOtherText: longOtherText,
      sexAtBirthOtherText: longOtherText,
      orientationOtherText: longOtherText,
      surveyComments: 's'.repeat(SURVEY_COMMENTS_MAX_LENGTH),
    };
    const v1 = validateDemographicSurvey(surveyFields);
    const v2 = validateDemographicSurvey_V2(surveyFields);

    expect(v2).toEqual(v1);
  });
  it('V2 validation matches V1 - min year of birth', () => {
    const surveyFields: DemographicSurveyV2 = {
      yearOfBirth: 1899, // Below the minimum year
    };
    const v1 = validateDemographicSurvey(surveyFields);
    const v2 = validateDemographicSurvey_V2(surveyFields);

    expect(v2).toEqual(v1);
  });
  it('V2 validation matches V1 - max year of birth', () => {
    const surveyFields: DemographicSurveyV2 = {
      yearOfBirth: new Date().getFullYear() + 1, // Above the maximum year
    };
    const v1 = validateDemographicSurvey(surveyFields);
    const v2 = validateDemographicSurvey_V2(surveyFields);

    expect(v2).toEqual(v1);
  });
});
