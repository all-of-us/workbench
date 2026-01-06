import { DemographicSurveyV2 } from 'generated/fetch';

import {
  NONE_FULLY_DESCRIBE_MAX_LENGTH,
  OTHER_DISABILITY_MAX_LENGTH,
  SURVEY_COMMENTS_MAX_LENGTH,
  validateDemographicSurvey,
} from './demographic-survey-v2-validation';

describe('DemographicsSurveyV2 form validation', () => {
  const happySurvey: DemographicSurveyV2 = {
    disabilityConcentrating: 'NO',
    disabilityWalking: 'NO',
    disabilityDressing: 'NO',
    disabilityErrands: 'NO',
    disabilityHearing: 'NO',
    disabilitySeeing: 'NO',
    yearOfBirth: 1990,
    education: 'COLLEGE_GRADUATE',
    disadvantaged: 'NO',
    ethnicCategories: ['WHITE', 'WHITE_EUROPEAN'],
    genderIdentities: ['MAN'],
    sexAtBirth: 'MALE',
    sexualOrientations: ['STRAIGHT'],
  };

  it('returns no errors for a happy path survey', () => {
    // Arrange
    const surveyFields: DemographicSurveyV2 = happySurvey;

    // Act
    const errors = validateDemographicSurvey(surveyFields);

    // Assert
    const expectedErrors = undefined; // no errors
    expect(errors).toEqual(expectedErrors);
  });

  it('returns errors for blank fields', () => {
    // Arrange
    const surveyFields: DemographicSurveyV2 = {};

    // Act
    const errors = validateDemographicSurvey(surveyFields);

    // Assert
    const expectedErrors: Record<string, string[]> = {
      disabilityConcentrating: [" Difficulty concentrating can't be blank"],
      disabilityWalking: [" Difficulty walking can't be blank"],
      disabilityDressing: [" Difficulty dressing can't be blank"],
      disabilityErrands: [" Difficulty doing errands can't be blank"],
      disabilityHearing: [" Difficulty hearing can't be blank"],
      disabilitySeeing: [" Difficulty seeing can't be blank"],
      yearOfBirth: [
        'You must either fill in your year of birth or check the corresponding "Prefer not to answer" checkbox ',
      ],
      education: ["Education can't be blank"],
      disadvantaged: ["Disadvantaged can't be blank"],
      ethnicCategories: ["Ethnic categories can't be blank"],
      genderIdentities: ["Gender identities can't be blank"],
      sexAtBirth: ["Sex at birth can't be blank"],
      sexualOrientations: ["Sexual orientations can't be blank"],
    };

    expect(errors).toEqual(expectedErrors);
  });

  it('returns errors for blank fields (null)', () => {
    // Arrange
    const surveyFields: DemographicSurveyV2 = {
      disabilityConcentrating: null,
      disabilityWalking: null,
      disabilityDressing: null,
      disabilityErrands: null,
      disabilityHearing: null,
      disabilitySeeing: null,
      yearOfBirth: null,
      education: null,
      disadvantaged: null,
      ethnicCategories: null,
      genderIdentities: null,
      sexAtBirth: null,
      sexualOrientations: null,
    };

    // Act
    const errors = validateDemographicSurvey(surveyFields);

    // Assert
    const expectedErrors: Record<string, string[]> = {
      disabilityConcentrating: [" Difficulty concentrating can't be blank"],
      disabilityWalking: [" Difficulty walking can't be blank"],
      disabilityDressing: [" Difficulty dressing can't be blank"],
      disabilityErrands: [" Difficulty doing errands can't be blank"],
      disabilityHearing: [" Difficulty hearing can't be blank"],
      disabilitySeeing: [" Difficulty seeing can't be blank"],
      yearOfBirth: [
        'You must either fill in your year of birth or check the corresponding "Prefer not to answer" checkbox ',
      ],
      education: ["Education can't be blank"],
      disadvantaged: ["Disadvantaged can't be blank"],
      ethnicCategories: ["Ethnic categories can't be blank"],
      genderIdentities: ["Gender identities can't be blank"],
      sexAtBirth: ["Sex at birth can't be blank"],
      sexualOrientations: ["Sexual orientations can't be blank"],
    };

    expect(errors).toEqual(expectedErrors);
  });

  it('returns max length errors', () => {
    // Arrange
    const longOtherText = 'a'.repeat(NONE_FULLY_DESCRIBE_MAX_LENGTH + 1);
    const surveyFields: DemographicSurveyV2 = {
      ...happySurvey,
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

    // Act
    const errors = validateDemographicSurvey(surveyFields);

    // Assert
    const expectedErrors: Record<string, string[]> = {
      disabilityOtherText: [
        'Disability other text can have no more than 200 characters.',
      ],
      ethnicityAiAnOtherText: [
        'Ethnicity ai an other text can have no more than 200 characters.',
      ],
      ethnicityAsianOtherText: [
        'Ethnicity asian other text can have no more than 200 characters.',
      ],
      ethnicityBlackOtherText: [
        'Ethnicity black other text can have no more than 200 characters.',
      ],
      ethnicityHispanicOtherText: [
        'Ethnicity hispanic other text can have no more than 200 characters.',
      ],
      ethnicityMeNaOtherText: [
        'Ethnicity me na other text can have no more than 200 characters.',
      ],
      ethnicityNhPiOtherText: [
        'Ethnicity nh pi other text can have no more than 200 characters.',
      ],
      ethnicityOtherText: [
        'Ethnicity other text can have no more than 200 characters.',
      ],
      ethnicityWhiteOtherText: [
        'Ethnicity white other text can have no more than 200 characters.',
      ],
      genderOtherText: [
        'Gender other text can have no more than 200 characters.',
      ],
      orientationOtherText: [
        'Orientation other text can have no more than 200 characters.',
      ],
      sexAtBirthOtherText: [
        'Sex at birth other text can have no more than 200 characters.',
      ],
    };
    expect(errors).toEqual(expectedErrors);
  });

  it('returns error for min year of birth', () => {
    // Arrange
    const surveyFields: DemographicSurveyV2 = {
      ...happySurvey,
      yearOfBirth: 1899, // Below the minimum year
    };

    // Act
    const errors = validateDemographicSurvey(surveyFields);

    // Assert
    const minYear = new Date().getFullYear() - 125;
    const expectedErrors: Record<string, string[]> = {
      yearOfBirth: [
        `Year of birth must be greater than or equal to ${minYear}`,
      ],
    };
    expect(errors).toEqual(expectedErrors);
  });

  it('returns error for max year of birth', () => {
    // Arrange
    const surveyFields: DemographicSurveyV2 = {
      ...happySurvey,
      yearOfBirth: new Date().getFullYear() + 1, // Above the maximum year
    };

    // Act
    const errors = validateDemographicSurvey(surveyFields);

    // Assert
    const maxYear = new Date().getFullYear();
    const expectedErrors: Record<string, string[]> = {
      yearOfBirth: [`Year of birth must be less than or equal to ${maxYear}`],
    };
    expect(errors).toEqual(expectedErrors);
  });
});
