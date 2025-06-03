import validate from 'validate.js';

import { DemographicSurveyV2 } from 'generated/fetch';

import {
  nonEmptyArray,
  optionalStringWithMaxLength,
  refinedObject,
  refineFields,
  requiredString,
  zodToValidateJS,
} from 'app/utils/zod-validators';
import { z } from 'zod';

export const maxYear = new Date().getFullYear();
export const minYear = maxYear - 125;

export const OTHER_DISABILITY_MAX_LENGTH = 200;
export const MAX_CHARACTERS_VALIDATION_MESSAGE =
  'can have no more than %{count} characters.';
export const SURVEY_COMMENTS_MAX_LENGTH = 1000;
export const NONE_FULLY_DESCRIBE_MAX_LENGTH = 200;
const NONE_FULLY_DESCRIBE_VALIDATION = {
  length: {
    maximum: NONE_FULLY_DESCRIBE_MAX_LENGTH,
    tooLong: MAX_CHARACTERS_VALIDATION_MESSAGE,
  },
};

export const validateDemographicSurvey = (
  demographicSurvey: DemographicSurveyV2
) => {
  validate.validators.nullBoolean = (v) =>
    v === true || v === false || v === null
      ? undefined
      : 'value must be selected';

  // Not well documented, but in Validate.JS, if your message is prefixed with a caret,
  // the field name will not be included with the message:
  // https://validatejs.org/docs/validate.html#section-47
  // https://github.com/ansman/validate.js/issues/68
  const yearOfBirth = demographicSurvey.yearOfBirthPreferNot
    ? {}
    : {
        yearOfBirth: {
          presence: {
            allowEmpty: false,
            message:
              '^You must either fill in your year of birth or check the corresponding ' +
              '"Prefer not to answer" checkbox ',
          },
          numericality: {
            onlyInteger: true,
            greaterThanOrEqualTo: minYear,
            lessThanOrEqualTo: maxYear,
          },
        },
      };

  const validationCheck = {
    ethnicCategories: { presence: { allowEmpty: false } },
    genderIdentities: { presence: { allowEmpty: false } },
    sexAtBirth: { presence: { allowEmpty: false } },
    sexualOrientations: { presence: { allowEmpty: false } },
    disabilityHearing: {
      presence: {
        allowEmpty: false,
        message: "^ Difficulty hearing can't be blank",
      },
    },
    disabilitySeeing: {
      presence: {
        allowEmpty: false,
        message: "^ Difficulty seeing can't be blank",
      },
    },
    disabilityConcentrating: {
      presence: {
        allowEmpty: false,
        message: "^ Difficulty concentrating can't be blank",
      },
    },
    disabilityWalking: {
      presence: {
        allowEmpty: false,
        message: "^ Difficulty walking can't be blank",
      },
    },
    disabilityDressing: {
      presence: {
        allowEmpty: false,
        message: "^ Difficulty dressing can't be blank",
      },
    },
    disabilityErrands: {
      presence: {
        allowEmpty: false,
        message: "^ Difficulty doing errands can't be blank",
      },
    },
    disabilityOtherText: {
      length: {
        maximum: OTHER_DISABILITY_MAX_LENGTH,
        tooLong: MAX_CHARACTERS_VALIDATION_MESSAGE,
      },
    },
    ...yearOfBirth,
    education: { presence: { allowEmpty: false } },
    disadvantaged: { presence: { allowEmpty: false } },
    surveyComments: {
      length: {
        maximum: SURVEY_COMMENTS_MAX_LENGTH,
        tooLong: MAX_CHARACTERS_VALIDATION_MESSAGE,
      },
    },
    ...[
      'ethnicityAiAnOtherText',
      'ethnicityAsianOtherText',
      'ethnicityBlackOtherText',
      'ethnicityHispanicOtherText',
      'ethnicityMeNaOtherText',
      'ethnicityNhPiOtherText',
      'ethnicityWhiteOtherText',
      'ethnicityOtherText',
      'genderOtherText',
      'sexAtBirthOtherText',
      'orientationOtherText',
    ].reduce(
      (otherTextValidations, k) => ({
        ...otherTextValidations,
        [k]: NONE_FULLY_DESCRIBE_VALIDATION,
      }),
      {}
    ),
  };
  return validate(demographicSurvey, validationCheck);
};

// V2 validation with zod ================================================================
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
        : z
            .number({ required_error: needYearOfBirthMsg })
            .int()
            .min(minYear, {
              message: `Year of birth must be greater than or equal to ${minYear}`,
            })
            .max(maxYear, {
              message: `Year of birth must be less than or equal to ${maxYear}`,
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
export const validateDemographicSurvey_V2 = (data: DemographicSurveyV2) =>
  zodToValidateJS(() => demographicSurveySchema.parse(data));
