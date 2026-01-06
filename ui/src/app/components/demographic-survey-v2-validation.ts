import { DemographicSurveyV2 } from 'generated/fetch';

import {
  nonEmptyArray,
  optionalStringWithMaxLength,
  refinedObject,
  refineFields,
  requiredNumber,
  requiredString,
  zodToValidateJS,
} from 'app/utils/zod-validators';
import { z } from 'zod';

export const getMaxYear = () => new Date().getFullYear();
export const getMinYear = () => getMaxYear() - 125;

export const OTHER_DISABILITY_MAX_LENGTH = 200;
export const MAX_CHARACTERS_VALIDATION_MESSAGE =
  'can have no more than %{count} characters.';
export const SURVEY_COMMENTS_MAX_LENGTH = 1000;
export const NONE_FULLY_DESCRIBE_MAX_LENGTH = 200;

/**
 * YES | NO | PREFER_NOT_TO_ANSWER
 **/
const requiredYesNoPreferNot = (msg: string = 'Required') =>
  requiredString(msg);
const optionalMaxText = (
  prefix?: string,
  max = NONE_FULLY_DESCRIBE_MAX_LENGTH
) => optionalStringWithMaxLength(max, prefix);

export const demographicSurveySchema = refinedObject<DemographicSurveyV2>(
  (fields, ctx) => {
    const noop = undefined;
    const needYearOfBirthMsg =
      'You must either fill in your year of birth or check the corresponding "Prefer not to answer" checkbox ';

    refineFields(fields, ctx, {
      ethnicCategories: nonEmptyArray(
        z.string(),
        "Ethnic categories can't be blank"
      ),
      genderIdentities: nonEmptyArray(
        z.string(),
        "Gender identities can't be blank"
      ),
      sexAtBirth: requiredString("Sex at birth can't be blank"),
      sexualOrientations: nonEmptyArray(
        z.string(),
        "Sexual orientations can't be blank"
      ),
      yearOfBirth: fields.yearOfBirthPreferNot
        ? noop
        : requiredNumber(needYearOfBirthMsg)
            .int()
            .min(getMinYear(), {
              message: `Year of birth must be greater than or equal to ${getMinYear()}`,
            })
            .max(getMaxYear(), {
              message: `Year of birth must be less than or equal to ${getMaxYear()}`,
            }),
      education: requiredString("Education can't be blank"),
      disadvantaged: requiredYesNoPreferNot("Disadvantaged can't be blank"),
      disabilityHearing: requiredYesNoPreferNot(
        " Difficulty hearing can't be blank"
      ),
      disabilitySeeing: requiredYesNoPreferNot(
        " Difficulty seeing can't be blank"
      ),
      disabilityConcentrating: requiredYesNoPreferNot(
        " Difficulty concentrating can't be blank"
      ),
      disabilityWalking: requiredYesNoPreferNot(
        " Difficulty walking can't be blank"
      ),
      disabilityDressing: requiredYesNoPreferNot(
        " Difficulty dressing can't be blank"
      ),
      disabilityErrands: requiredYesNoPreferNot(
        " Difficulty doing errands can't be blank"
      ),
      disabilityOtherText: optionalMaxText(
        'Disability other text',
        OTHER_DISABILITY_MAX_LENGTH
      ),
      ethnicityAiAnOtherText: optionalMaxText('Ethnicity ai an other text'),
      ethnicityAsianOtherText: optionalMaxText('Ethnicity asian other text'),
      ethnicityBlackOtherText: optionalMaxText('Ethnicity black other text'),
      ethnicityHispanicOtherText: optionalMaxText(
        'Ethnicity hispanic other text'
      ),
      ethnicityMeNaOtherText: optionalMaxText('Ethnicity me na other text'),
      ethnicityNhPiOtherText: optionalMaxText('Ethnicity nh pi other text'),
      ethnicityWhiteOtherText: optionalMaxText('Ethnicity white other text'),
      ethnicityOtherText: optionalMaxText('Ethnicity other text'),
      genderOtherText: optionalMaxText('Gender other text'),
      sexAtBirthOtherText: optionalMaxText('Sex at birth other text'),
      orientationOtherText: optionalMaxText('Orientation other text'),
      surveyComments: optionalMaxText(
        'Survey comments',
        SURVEY_COMMENTS_MAX_LENGTH
      ),
    });
  }
);

// Helper function to validate demographic survey data
export const validateDemographicSurvey = (data: DemographicSurveyV2) =>
  zodToValidateJS(() => demographicSurveySchema.parse(data));
