import * as React from 'react';
import { useEffect, useState } from 'react';
import * as fp from 'lodash/fp';

import {
  EducationV2,
  EthnicCategory,
  GenderIdentityV2,
  Profile,
  SexAtBirthV2,
  SexualOrientationV2,
} from 'generated/fetch';

import { FlexColumn, FlexRow } from 'app/components/flex';
import { withProfileErrorModal } from 'app/components/with-error-modal-wrapper';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import { reactStyles } from 'app/utils';
import {
  DEMOGRAPHIC_SURVEY_COMMENTS,
  DISABILITY_CONCENTRATING,
  DISABILITY_DRESSING,
  DISABILITY_ERRANDS,
  DISABILITY_HEARING,
  DISABILITY_OTHER,
  DISABILITY_SEEING,
  DISABILITY_WALKING,
  DISADVANTAGED,
  EDUCATION,
  ETHNIC_CATEGORIES,
  GENDER_IDENTITIES,
  SEX_AT_BIRTH,
  SEXUAL_ORIENTATIONS,
  YEAR_OF_BIRTH,
} from 'app/utils/constants';

import {
  maxYear,
  minYear,
  NONE_FULLY_DESCRIBE_MAX_LENGTH,
  OTHER_DISABILITY_MAX_LENGTH,
  SURVEY_COMMENTS_MAX_LENGTH,
  validateDemographicSurvey,
} from './demographic-survey-v2-validation';
import {
  CheckBox,
  NumberInput,
  TextAreaWithLengthValidationMessage,
} from './inputs';
import { MultipleChoiceQuestion } from './multiple-choice-question';
import { AoU } from './text-wrappers';
import { WithSpinnerOverlayProps } from './with-spinner-overlay';
import { YesNoOptionalQuestion } from './yes-no-optional-question';

export const possiblePreferNotToAnswerErrors = [
  ETHNIC_CATEGORIES,
  GENDER_IDENTITIES,
  SEX_AT_BIRTH,
  SEXUAL_ORIENTATIONS,
  DISABILITY_HEARING,
  DISABILITY_SEEING,
  DISABILITY_CONCENTRATING,
  DISABILITY_WALKING,
  DISABILITY_DRESSING,
  DISABILITY_ERRANDS,
  EDUCATION,
  DISADVANTAGED,
];

export const questionsIndex = {
  [ETHNIC_CATEGORIES]: 1,
  [GENDER_IDENTITIES]: 2,
  [SEX_AT_BIRTH]: 3,
  [SEXUAL_ORIENTATIONS]: 4,
  [DISABILITY_HEARING]: 5,
  [DISABILITY_SEEING]: 6,
  [DISABILITY_CONCENTRATING]: 7,
  [DISABILITY_WALKING]: 8,
  [DISABILITY_DRESSING]: 9,
  [DISABILITY_ERRANDS]: 10,
  [DISABILITY_OTHER]: 11,
  [YEAR_OF_BIRTH]: 12,
  [EDUCATION]: 13,
  [DISADVANTAGED]: 14,
  [DEMOGRAPHIC_SURVEY_COMMENTS]: 15,
};

const styles = reactStyles({
  question: {
    fontSize: 18,
    fontWeight: 'bold',
    color: colors.primary,
    lineHeight: '1.875rem',
  },
  answer: { margin: '0.0rem 0.375rem', color: colors.primary },
});

const NONE_FULLY_DESCRIBE = 'None of these fully describe me';
const NONE_FULLY_DESCRIBE_PLACEHOLDER = 'Please specify (optional)';
const TWO_SPIRIT_DISABLED_TEXT =
  'unique to people of American Indian and Alaska Native ' +
  'ancestry. If this applies to you, please update your selection in the ' +
  '"Racial and Ethnic" section.';
const TWO_SPIRIT_IDENTITY_DISABLED_TEXT =
  'Two Spirit is an identity ' + TWO_SPIRIT_DISABLED_TEXT;
const TWO_SPIRIT_SEXUAL_ORIENTATION_DISABLED_TEXT =
  'Two Spirit is an orientation ' + TWO_SPIRIT_DISABLED_TEXT;

interface DemographicSurveyProps extends WithSpinnerOverlayProps {
  onError: (any) => void;
  onUpdate: (string, any) => void;
  profile: Profile;
}

export const DemographicSurvey = fp.flow(withProfileErrorModal)(
  (props: DemographicSurveyProps) => {
    const { onError, onUpdate, profile } = props;
    const { demographicSurveyV2: survey } = profile;

    const [isAian, setIsAian] = useState(
      survey.ethnicCategories.includes(EthnicCategory.AI_AN)
    );
    const [showAsianOptions, setShowAsianOptions] = useState(
      survey.ethnicCategories.includes(EthnicCategory.ASIAN)
    );
    const [showAiAnOptions, setShowAiAnOptions] = useState(
      survey.ethnicCategories.includes(EthnicCategory.AI_AN)
    );
    const [showBlackOptions, setShowBlackOptions] = useState(
      survey.ethnicCategories.includes(EthnicCategory.BLACK)
    );
    const [showHispanicOptions, setShowHispanicOptions] = useState(
      survey.ethnicCategories.includes(EthnicCategory.HISPANIC)
    );
    const [showMeNaOptions, setShowMeNaOptions] = useState(
      survey.ethnicCategories.includes(EthnicCategory.MENA)
    );
    const [showNhPiOptions, setShowNhPiOptions] = useState(
      survey.ethnicCategories.includes(EthnicCategory.NHPI)
    );
    const [showWhiteOptions, setShowWhiteOptions] = useState(
      survey.ethnicCategories.includes(EthnicCategory.WHITE)
    );

    const setShowOptionsFunctionList = [
      setShowAsianOptions,
      setShowAiAnOptions,
      setShowBlackOptions,
      setShowHispanicOptions,
      setShowMeNaOptions,
      setShowNhPiOptions,
      setShowWhiteOptions,
    ];

    useEffect(() => {
      onError(validateDemographicSurvey(survey));
    }, [profile]);

    useEffect(() => {
      const includesAiAN = survey.ethnicCategories.includes(
        EthnicCategory.AI_AN
      );
      const isAiAnChanged = includesAiAN !== isAian;

      if (isAiAnChanged && !includesAiAN) {
        onUpdate(
          'sexualOrientations',
          survey.sexualOrientations.filter(
            (so) => so !== SexualOrientationV2.TWO_SPIRIT
          )
        );
        onUpdate(
          'genderIdentities',
          survey.genderIdentities.filter(
            (so) => so !== GenderIdentityV2.TWO_SPIRIT
          )
        );
      }
      if (isAiAnChanged) {
        setIsAian(includesAiAN);
      }
    }, [survey, isAian]);

    const getQuestionLabel = (key, label) => {
      return questionsIndex[key] + '. ' + label;
    };

    const disadvantagedBackgroundQuestion = (
      <div>
        {getQuestionLabel(
          DISADVANTAGED,
          `Are you an individual from a disadvantaged background, as `
        )}
        <a
          target='_blank'
          href='https://extramural-diversity.nih.gov/diversity-matters/disadvantaged-backgrounds'
        >
          defined by NIH Diversity in Extramural Programs?
        </a>
      </div>
    );

    return (
      <FlexColumn>
        <div
          id='demographics-survey'
          style={{
            backgroundColor: colorWithWhiteness(colors.accent, 0.75),
            padding: '1.5rem',
            borderRadius: '5px',
            color: colors.primary,
            marginBottom: '1.5rem',
            marginTop: '1.5rem',
          }}
        >
          <div>
            The <AoU /> Research Program is dedicated to cultivating a diverse
            research community and building an inclusive platform. Your answers
            to these questions will help us learn more about who is using the
            research platform.
          </div>
          <div style={{ marginTop: '1.5rem' }}>
            Your privacy is important to us. Your individual responses will not
            be displayed on your researcher profile and will only be accessible
            to a limited number of authorized <AoU /> personnel. <AoU /> may
            externally report de-identified, aggregate data on the demographics
            of individuals using the Researcher Workbench.
          </div>
        </div>
        <FlexColumn>
          <MultipleChoiceQuestion
            question={getQuestionLabel(
              ETHNIC_CATEGORIES,
              'Which Racial and Ethnic categories describe you?'
            )}
            label='Racial and/or Ethnic Identity/Identities'
            options={[
              {
                label: 'American Indian or Alaska Native (AIAN)',
                value: EthnicCategory.AI_AN,
                subOptions: [
                  {
                    label: 'American Indian',
                    value: EthnicCategory.AI_AN_AMERICAN_INDIAN,
                  },
                  {
                    label: 'Alaska Native',
                    value: EthnicCategory.AI_AN_ALASKA_NATIVE,
                  },
                  {
                    label: 'Central or South American Indian',
                    value: EthnicCategory.AI_AN_CENTRAL_SOUTH,
                  },
                  {
                    label: NONE_FULLY_DESCRIBE,
                    value: EthnicCategory.AI_AN_OTHER,
                    showInput: true,
                    otherText: survey.ethnicityAiAnOtherText,
                    otherTextMaxLength: NONE_FULLY_DESCRIBE_MAX_LENGTH,
                    otherTextPlaceholder: NONE_FULLY_DESCRIBE_PLACEHOLDER,
                    onChange: (checked) => {
                      if (!checked) {
                        onUpdate('ethnicityAiAnOtherText', null);
                      }
                    },
                    onChangeOtherText: (value) =>
                      onUpdate('ethnicityAiAnOtherText', value),
                  },
                ],
                showSubOptions: showAiAnOptions,
                onChange: (checked) => {
                  if (!checked) {
                    onUpdate('ethnicityAiAnOtherText', null);
                  }
                  setShowAiAnOptions(checked);
                },
                onExpand: () => setShowAiAnOptions(!showAiAnOptions),
              },

              {
                label: 'Asian',
                value: EthnicCategory.ASIAN,
                subOptions: [
                  { label: 'Asian Indian', value: EthnicCategory.ASIAN_INDIAN },
                  { label: 'Cambodian', value: EthnicCategory.ASIAN_CAMBODIAN },
                  { label: 'Chinese', value: EthnicCategory.ASIAN_CHINESE },
                  { label: 'Filipino', value: EthnicCategory.ASIAN_FILIPINO },
                  { label: 'Hmong', value: EthnicCategory.ASIAN_HMONG },
                  { label: 'Japanese', value: EthnicCategory.ASIAN_JAPANESE },
                  { label: 'Korean', value: EthnicCategory.ASIAN_KOREAN },
                  { label: 'Lao', value: EthnicCategory.ASIAN_LAO },
                  { label: 'Pakistani', value: EthnicCategory.ASIAN_PAKISTANI },
                  {
                    label: 'Vietnamese',
                    value: EthnicCategory.ASIAN_VIETNAMESE,
                  },
                  {
                    label: NONE_FULLY_DESCRIBE,
                    value: EthnicCategory.ASIAN_OTHER,
                    showInput: true,
                    otherText: survey.ethnicityAsianOtherText,
                    otherTextMaxLength: NONE_FULLY_DESCRIBE_MAX_LENGTH,
                    otherTextPlaceholder: NONE_FULLY_DESCRIBE_PLACEHOLDER,
                    onChange: (value) => {
                      if (!value) {
                        onUpdate('ethnicityAsianOtherText', null);
                      }
                    },
                    onChangeOtherText: (value) =>
                      onUpdate('ethnicityAsianOtherText', value),
                  },
                ],
                showSubOptions: showAsianOptions,
                onChange: (checked) => {
                  if (!checked) {
                    onUpdate('ethnicityAsianOtherText', null);
                  }
                  setShowAsianOptions(checked);
                },
                onExpand: () => setShowAsianOptions(!showAsianOptions),
              },
              {
                label: 'Black or African American',
                value: EthnicCategory.BLACK,
                subOptions: [
                  {
                    label: 'African American',
                    value: EthnicCategory.BLACK_AA,
                  },
                  { label: 'Barbadian', value: EthnicCategory.BLACK_BARBADIAN },
                  { label: 'Caribbean', value: EthnicCategory.BLACK_CARIBBEAN },
                  { label: 'Ethiopian', value: EthnicCategory.BLACK_ETHIOPIAN },
                  { label: 'Ghanaian', value: EthnicCategory.BLACK_GHANAIAN },
                  { label: 'Haitian', value: EthnicCategory.BLACK_HAITIAN },
                  { label: 'Jamaican', value: EthnicCategory.BLACK_JAMAICAN },
                  { label: 'Liberian', value: EthnicCategory.BLACK_LIBERIAN },
                  { label: 'Nigerian', value: EthnicCategory.BLACK_NIGERIAN },
                  { label: 'Somali', value: EthnicCategory.BLACK_SOMALI },
                  {
                    label: 'South African',
                    value: EthnicCategory.BLACK_SOUTH_AFRICAN,
                  },
                  {
                    label: NONE_FULLY_DESCRIBE,
                    value: EthnicCategory.BLACK_OTHER,
                    showInput: true,
                    otherText: survey.ethnicityBlackOtherText,
                    otherTextMaxLength: NONE_FULLY_DESCRIBE_MAX_LENGTH,
                    otherTextPlaceholder: NONE_FULLY_DESCRIBE_PLACEHOLDER,
                    onChange: (value) => {
                      if (!value) {
                        onUpdate('ethnicityBlackOtherText', null);
                      }
                    },
                    onChangeOtherText: (value) =>
                      onUpdate('ethnicityBlackOtherText', value),
                  },
                ],
                showSubOptions: showBlackOptions,
                onChange: (checked) => {
                  if (!checked) {
                    onUpdate('ethnicityBlackOtherText', null);
                  }
                  setShowBlackOptions(checked);
                },
                onExpand: () => setShowBlackOptions(!showBlackOptions),
              },
              {
                label: 'Hispanic or Latino or Spanish Origin',
                value: EthnicCategory.HISPANIC,
                subOptions: [
                  {
                    label: 'Colombian',
                    value: EthnicCategory.HISPANIC_COLOMBIAN,
                  },
                  { label: 'Cuban', value: EthnicCategory.HISPANIC_CUBAN },
                  {
                    label: 'Dominican',
                    value: EthnicCategory.HISPANIC_DOMINICAN,
                  },
                  {
                    label: 'Ecuadorian',
                    value: EthnicCategory.HISPANIC_ECUADORIAN,
                  },
                  {
                    label: 'Honduran',
                    value: EthnicCategory.HISPANIC_HONDURAN,
                  },
                  {
                    label: 'Mexican or Mexican American',
                    value: EthnicCategory.HISPANIC_MEXICAN,
                  },
                  {
                    label: 'Puerto Rican',
                    value: EthnicCategory.HISPANIC_PUERTO_RICAN,
                  },
                  {
                    label: 'Salvadoran',
                    value: EthnicCategory.HISPANIC_SALVADORAN,
                  },
                  { label: 'Spanish', value: EthnicCategory.HISPANIC_SPANISH },
                  {
                    label: NONE_FULLY_DESCRIBE,
                    value: EthnicCategory.HISPANIC_OTHER,
                    showInput: true,
                    otherText: survey.ethnicityHispanicOtherText,
                    otherTextMaxLength: NONE_FULLY_DESCRIBE_MAX_LENGTH,
                    otherTextPlaceholder: NONE_FULLY_DESCRIBE_PLACEHOLDER,
                    onChange: (value) => {
                      if (!value) {
                        onUpdate('ethnicityHispanicOtherText', null);
                      }
                    },
                    onChangeOtherText: (value) =>
                      onUpdate('ethnicityHispanicOtherText', value),
                  },
                ],
                showSubOptions: showHispanicOptions,
                onChange: (checked) => {
                  if (!checked) {
                    onUpdate('ethnicityHispanicOtherText', null);
                  }
                  setShowHispanicOptions(checked);
                },
                onExpand: () => setShowHispanicOptions(!showHispanicOptions),
              },
              {
                label: 'Middle Eastern or North African',
                value: EthnicCategory.MENA,
                subOptions: [
                  {
                    label: 'Afghan',
                    value: EthnicCategory.MENA_AFGHAN,
                  },
                  { label: 'Algerian', value: EthnicCategory.MENA_ALGERIAN },
                  { label: 'Egyptian', value: EthnicCategory.MENA_EGYPTIAN },
                  {
                    label: 'Iranian',
                    value: EthnicCategory.MENA_IRANIAN,
                  },
                  { label: 'Iraqi', value: EthnicCategory.MENA_IRAQI },
                  { label: 'Israeli', value: EthnicCategory.MENA_ISRAELI },
                  {
                    label: 'Lebanese',
                    value: EthnicCategory.MENA_LEBANESE,
                  },
                  {
                    label: 'Moroccan',
                    value: EthnicCategory.MENA_MOROCCAN,
                  },
                  { label: 'Syrian', value: EthnicCategory.MENA_SYRIAN },
                  { label: 'Tunisian', value: EthnicCategory.MENA_TUNISIAN },
                  {
                    label: NONE_FULLY_DESCRIBE,
                    value: EthnicCategory.MENA_OTHER,
                    showInput: true,
                    otherText: survey.ethnicityMeNaOtherText,
                    otherTextMaxLength: NONE_FULLY_DESCRIBE_MAX_LENGTH,
                    otherTextPlaceholder: NONE_FULLY_DESCRIBE_PLACEHOLDER,
                    onChange: (value) => {
                      if (!value) {
                        onUpdate('ethnicityMeNaOtherText', null);
                      }
                    },
                    onChangeOtherText: (value) =>
                      onUpdate('ethnicityMeNaOtherText', value),
                  },
                ],
                showSubOptions: showMeNaOptions,
                onChange: (checked) => {
                  if (!checked) {
                    onUpdate('ethnicityMeNaOtherText', null);
                  }
                  setShowMeNaOptions(checked);
                },
                onExpand: () => setShowMeNaOptions(!showMeNaOptions),
              },
              {
                label: 'Native Hawaiian or Other Pacific Islander',
                value: EthnicCategory.NHPI,
                subOptions: [
                  {
                    label: 'Chamorro',
                    value: EthnicCategory.NHPI_CHAMORRO,
                  },
                  { label: 'Chuukese', value: EthnicCategory.NHPI_CHUUKESE },
                  { label: 'Fijian', value: EthnicCategory.NHPI_FIJIAN },
                  {
                    label: 'Marshallese',
                    value: EthnicCategory.NHPI_MARSHALLESE,
                  },
                  {
                    label: 'Native Hawaiian',
                    value: EthnicCategory.NHPI_HAWAIIAN,
                  },
                  { label: 'Palauan', value: EthnicCategory.NHPI_PALAUAN },
                  {
                    label: 'Samoan',
                    value: EthnicCategory.NHPI_SAMOAN,
                  },
                  {
                    label: 'Tahitian',
                    value: EthnicCategory.NHPI_TAHITIAN,
                  },
                  { label: 'Tongan', value: EthnicCategory.NHPI_TONGAN },
                  {
                    label: NONE_FULLY_DESCRIBE,
                    value: EthnicCategory.NHPI_OTHER,
                    showInput: true,
                    otherText: survey.ethnicityNhPiOtherText,
                    otherTextMaxLength: NONE_FULLY_DESCRIBE_MAX_LENGTH,
                    otherTextPlaceholder: NONE_FULLY_DESCRIBE_PLACEHOLDER,
                    onChange: (value) => {
                      if (!value) {
                        onUpdate('ethnicityNhPiOtherText', null);
                      }
                    },
                    onChangeOtherText: (value) =>
                      onUpdate('ethnicityNhPiOtherText', value),
                  },
                ],
                showSubOptions: showNhPiOptions,
                onChange: (checked) => {
                  if (!checked) {
                    onUpdate('ethnicityNhPiOtherText', null);
                  }
                  setShowNhPiOptions(checked);
                },
                onExpand: () => setShowNhPiOptions(!showNhPiOptions),
              },
              {
                label: 'White',
                value: EthnicCategory.WHITE,
                subOptions: [
                  {
                    label: 'Dutch',
                    value: EthnicCategory.WHITE_DUTCH,
                  },
                  { label: 'English', value: EthnicCategory.WHITE_ENGLISH },
                  { label: 'European', value: EthnicCategory.WHITE_EUROPEAN },
                  {
                    label: 'French',
                    value: EthnicCategory.WHITE_FRENCH,
                  },
                  {
                    label: 'German',
                    value: EthnicCategory.WHITE_GERMAN,
                  },
                  { label: 'Irish', value: EthnicCategory.WHITE_IRISH },
                  {
                    label: 'Italian',
                    value: EthnicCategory.WHITE_ITALIAN,
                  },
                  {
                    label: 'Norwegian',
                    value: EthnicCategory.WHITE_NORWEGIAN,
                  },
                  { label: 'Polish', value: EthnicCategory.WHITE_POLISH },
                  { label: 'Scottish', value: EthnicCategory.WHITE_SCOTTISH },
                  { label: 'Spanish', value: EthnicCategory.WHITE_SPANISH },
                  {
                    label: NONE_FULLY_DESCRIBE,
                    value: EthnicCategory.WHITE_OTHER,
                    showInput: true,
                    otherText: survey.ethnicityWhiteOtherText,
                    otherTextMaxLength: NONE_FULLY_DESCRIBE_MAX_LENGTH,
                    otherTextPlaceholder: NONE_FULLY_DESCRIBE_PLACEHOLDER,
                    onChange: (value) => {
                      if (!value) {
                        onUpdate('ethnicityWhiteOtherText', null);
                      }
                    },
                    onChangeOtherText: (value) =>
                      onUpdate('ethnicityWhiteOtherText', value),
                  },
                ],
                showSubOptions: showWhiteOptions,
                onChange: (checked) => {
                  if (!checked) {
                    onUpdate('ethnicityWhiteOtherText', null);
                  }
                  setShowWhiteOptions(checked);
                },
                onExpand: () => {
                  setShowWhiteOptions(!showWhiteOptions);
                },
              },
              {
                label: 'Prefer not to answer',
                value: EthnicCategory.PREFER_NOT_TO_ANSWER,
                onChange: (checked) => {
                  onUpdate(
                    'ethnicCategories',
                    checked ? [EthnicCategory.PREFER_NOT_TO_ANSWER] : []
                  );
                  if (checked) {
                    [
                      'ethnicityAiAnOtherText',
                      'ethnicityAsianOtherText',
                      'ethnicityBlackOtherText',
                      'ethnicityHispanicOtherText',
                      'ethnicityMeNaOtherText',
                      'ethnicityNhPiOtherText',
                      'ethnicityWhiteOtherText',
                      'ethnicityOtherText',
                    ].map((fieldName) => onUpdate(fieldName, null));
                  }
                },
              },
              {
                label: NONE_FULLY_DESCRIBE,
                value: EthnicCategory.OTHER,
                showInput: true,
                otherText: survey.ethnicityOtherText,
                otherTextMaxLength: NONE_FULLY_DESCRIBE_MAX_LENGTH,
                otherTextPlaceholder: NONE_FULLY_DESCRIBE_PLACEHOLDER,
                onChange: (value) => {
                  if (!value) {
                    onUpdate('ethnicityOtherText', null);
                  }
                },
                onChangeOtherText: (value) =>
                  onUpdate('ethnicityOtherText', value),
              },
            ]}
            multiple
            selected={survey.ethnicCategories}
            onChange={(value) =>
              onUpdate(
                'ethnicCategories',
                value.filter((v) => v !== EthnicCategory.PREFER_NOT_TO_ANSWER)
              )
            }
            enableExpansionControls
            onCollapseAll={() =>
              setShowOptionsFunctionList.forEach((fn) => fn(false))
            }
            onExpandAll={() =>
              setShowOptionsFunctionList.forEach((fn) => fn(true))
            }
          />
          <MultipleChoiceQuestion
            question={getQuestionLabel(
              GENDER_IDENTITIES,
              'What terms best express how you describe your current gender identity?'
            )}
            label='Gender Identity/Identities'
            options={[
              { label: 'Genderqueer', value: GenderIdentityV2.GENDERQUEER },
              { label: 'Man', value: GenderIdentityV2.MAN },
              { label: 'Non-binary', value: GenderIdentityV2.NON_BINARY },
              {
                label: 'Trans man/Transgender man',
                value: GenderIdentityV2.TRANS_MAN,
              },
              {
                label: 'Trans woman/Transgender woman',
                value: GenderIdentityV2.TRANS_WOMAN,
              },
              {
                label: 'Two Spirit',
                value: GenderIdentityV2.TWO_SPIRIT,
                disabled: !isAian,
                disabledText: TWO_SPIRIT_IDENTITY_DISABLED_TEXT,
              },
              {
                label: 'Woman',
                value: GenderIdentityV2.WOMAN,
              },
              {
                label: 'Questioning or unsure of my gender identity',
                value: GenderIdentityV2.QUESTIONING,
              },
              {
                label: 'Prefer not to answer',
                value: GenderIdentityV2.PREFER_NOT_TO_ANSWER,
                onChange: (checked) => {
                  onUpdate(
                    'genderIdentities',
                    checked ? [GenderIdentityV2.PREFER_NOT_TO_ANSWER] : []
                  );
                  if (checked) {
                    onUpdate('genderOtherText', null);
                  }
                },
              },
              {
                label: NONE_FULLY_DESCRIBE,
                value: GenderIdentityV2.OTHER,
                showInput: true,
                otherText: survey.genderOtherText,
                otherTextMaxLength: NONE_FULLY_DESCRIBE_MAX_LENGTH,
                otherTextPlaceholder: NONE_FULLY_DESCRIBE_PLACEHOLDER,
                onChange: (value) => {
                  if (!value) {
                    onUpdate('genderOtherText', null);
                  }
                },
                onChangeOtherText: (value) =>
                  onUpdate('genderOtherText', value),
              },
            ]}
            multiple
            selected={survey.genderIdentities}
            onChange={(value) =>
              onUpdate(
                GENDER_IDENTITIES,
                value.filter((v) => v !== GenderIdentityV2.PREFER_NOT_TO_ANSWER)
              )
            }
          />
          <MultipleChoiceQuestion
            question={getQuestionLabel(
              SEX_AT_BIRTH,
              'What was the sex assigned to you at birth, such as on your original birth certificate?'
            )}
            label='Sex Assigned at Birth'
            options={[
              { label: 'Female', value: SexAtBirthV2.FEMALE },
              { label: 'Intersex', value: SexAtBirthV2.INTERSEX },
              { label: 'Male', value: SexAtBirthV2.MALE },
              {
                label: 'Prefer not to answer',
                value: SexAtBirthV2.PREFER_NOT_TO_ANSWER,
              },
              {
                label: NONE_FULLY_DESCRIBE,
                value: SexAtBirthV2.OTHER,
                showInput: true,
                otherText: survey.sexAtBirthOtherText,
                otherTextMaxLength: NONE_FULLY_DESCRIBE_MAX_LENGTH,
                otherTextPlaceholder: NONE_FULLY_DESCRIBE_PLACEHOLDER,
                onChange: (value) => {
                  if (!value) {
                    onUpdate('sexAtBirthOtherText', null);
                  }
                },
                onChangeOtherText: (value) =>
                  onUpdate('sexAtBirthOtherText', value),
              },
            ]}
            selected={survey.sexAtBirth}
            onChange={(value) => {
              if (
                survey.sexAtBirth === SexAtBirthV2.OTHER &&
                value !== SexAtBirthV2.OTHER
              ) {
                onUpdate('sexAtBirthOtherText', null);
              }
              onUpdate(SEX_AT_BIRTH, value);
            }}
          />
          <MultipleChoiceQuestion
            question={getQuestionLabel(
              SEXUAL_ORIENTATIONS,
              'What terms best express how you describe your current sexual orientation?'
            )}
            label='Sexual Orientation(s)'
            options={[
              { label: 'Asexual', value: SexualOrientationV2.ASEXUAL },
              { label: 'Bisexual', value: SexualOrientationV2.BISEXUAL },
              { label: 'Gay', value: SexualOrientationV2.GAY },
              { label: 'Lesbian', value: SexualOrientationV2.LESBIAN },
              {
                label: 'Polysexual, omnisexual, or pansexual',
                value: SexualOrientationV2.POLYSEXUAL,
              },
              { label: 'Queer', value: SexualOrientationV2.QUEER },
              {
                label: 'Same-gender loving',
                value: SexualOrientationV2.SAME_GENDER,
              },
              {
                label: 'Straight or heterosexual',
                value: SexualOrientationV2.STRAIGHT,
              },
              {
                label: 'Two Spirit',
                value: SexualOrientationV2.TWO_SPIRIT,
                disabled: !isAian,
                disabledText: TWO_SPIRIT_SEXUAL_ORIENTATION_DISABLED_TEXT,
              },
              {
                label: 'Questioning or unsure of my sexual orientation',
                value: SexualOrientationV2.QUESTIONING,
              },
              {
                label: 'Prefer not to answer',
                value: SexualOrientationV2.PREFER_NOT_TO_ANSWER,
                onChange: (checked) => {
                  onUpdate(
                    'sexualOrientations',
                    checked ? [SexualOrientationV2.PREFER_NOT_TO_ANSWER] : []
                  );
                  if (checked) {
                    onUpdate('orientationOtherText', null);
                  }
                },
              },
              {
                label: NONE_FULLY_DESCRIBE,
                value: SexualOrientationV2.OTHER,
                showInput: true,
                otherText: survey.orientationOtherText,
                otherTextMaxLength: NONE_FULLY_DESCRIBE_MAX_LENGTH,
                otherTextPlaceholder: NONE_FULLY_DESCRIBE_PLACEHOLDER,
                onChange: (value) => {
                  if (!value) {
                    onUpdate('orientationOtherText', null);
                  }
                },
                onChangeOtherText: (value) =>
                  onUpdate('orientationOtherText', value),
              },
            ]}
            multiple
            selected={survey.sexualOrientations}
            onChange={(value) =>
              onUpdate(
                SEXUAL_ORIENTATIONS,
                value.filter(
                  (v) => v !== SexualOrientationV2.PREFER_NOT_TO_ANSWER
                )
              )
            }
          />
          <YesNoOptionalQuestion
            question={getQuestionLabel(
              DISABILITY_HEARING,
              'Are you deaf or do you have serious difficulty hearing?'
            )}
            selected={survey.disabilityHearing}
            onChange={(value) => onUpdate(DISABILITY_HEARING, value)}
          />
          <YesNoOptionalQuestion
            question={getQuestionLabel(
              DISABILITY_SEEING,
              'Are you blind or do you have serious difficulty seeing, even when wearing glasses?'
            )}
            selected={survey.disabilitySeeing}
            onChange={(value) => onUpdate(DISABILITY_SEEING, value)}
          />
          <YesNoOptionalQuestion
            question={getQuestionLabel(
              DISABILITY_CONCENTRATING,
              'Because of a physical, cognitive, or emotional condition, do you have serious ' +
                'difficulty concentrating, remembering, or making decisions?'
            )}
            selected={survey.disabilityConcentrating}
            onChange={(value) => onUpdate(DISABILITY_CONCENTRATING, value)}
          />
          <YesNoOptionalQuestion
            question={getQuestionLabel(
              DISABILITY_WALKING,
              'Do you have serious difficulty walking or climbing stairs?'
            )}
            selected={survey.disabilityWalking}
            onChange={(value) => onUpdate(DISABILITY_WALKING, value)}
          />
          <YesNoOptionalQuestion
            question={getQuestionLabel(
              DISABILITY_DRESSING,
              'Do you have difficulty dressing or bathing?'
            )}
            selected={survey.disabilityDressing}
            onChange={(value) => onUpdate(DISABILITY_DRESSING, value)}
          />
          <YesNoOptionalQuestion
            question={getQuestionLabel(
              DISABILITY_ERRANDS,
              'Because of a physical, mental, or emotional condition, do you have difficulty ' +
                "doing errands alone such as visiting doctor's office or shopping?"
            )}
            selected={survey.disabilityErrands}
            onChange={(value) => onUpdate(DISABILITY_ERRANDS, value)}
          />
          <FlexColumn style={{ marginBottom: '1.5rem' }}>
            <div
              style={{ ...styles.question, flex: 1, marginBottom: '0.375rem' }}
            >
              {getQuestionLabel(
                DISABILITY_OTHER,
                'Do you have a physical, cognitive, and/or emotional condition ' +
                  'that substantially limits one or more life activities not ' +
                  'specified through the above questions, and want to share more? ' +
                  'Please describe.'
              )}
            </div>
            <TextAreaWithLengthValidationMessage
              id='disability-other-text'
              onChange={(value) => onUpdate('disabilityOtherText', value)}
              initialText={survey.disabilityOtherText || ''}
              textBoxStyleOverrides={{ width: '100%' }}
              heightOverride={{ height: '7.5rem' }}
              maxCharacters={OTHER_DISABILITY_MAX_LENGTH}
            />
          </FlexColumn>
          <div style={styles.question}>
            {getQuestionLabel(YEAR_OF_BIRTH, 'Year of birth')}
          </div>
          <FlexRow style={{ alignItems: 'center', marginBottom: '1.5rem' }}>
            <NumberInput
              onChange={(value) => onUpdate(YEAR_OF_BIRTH, value)}
              disabled={survey.yearOfBirthPreferNot}
              min={minYear}
              max={maxYear}
              value={survey.yearOfBirth || ''}
              style={{ width: '6rem' }}
            />
            <CheckBox
              checked={survey.yearOfBirthPreferNot}
              onChange={(value) => {
                onUpdate(YEAR_OF_BIRTH, null);
                onUpdate('yearOfBirthPreferNot', value);
              }}
              style={{ marginLeft: '1.5rem' }}
            />
            <label
              htmlFor='show-again-checkbox'
              style={{
                ...styles.answer,
                marginLeft: '8px',
                marginRight: 'auto',
              }}
            >
              Prefer not to answer
            </label>
          </FlexRow>
          <MultipleChoiceQuestion
            question={getQuestionLabel(EDUCATION, 'Highest Level of Education')}
            label='Highest Level of Education Completed'
            options={[
              {
                label: 'Never attended school/no formal education',
                value: EducationV2.NO_EDUCATION,
              },
              {
                label:
                  'Primary/Middle School/High School (Grades 1 through 12/GED)',
                value: EducationV2.GRADES_1_12,
              },
              {
                label:
                  'Some college, Associate Degree or Technical School (1 to 3 years) or current undergraduate student',
                value: EducationV2.UNDERGRADUATE,
              },
              {
                label:
                  'College graduate (4 years or more) or current post-graduate trainee',
                value: EducationV2.COLLEGE_GRADUATE,
              },

              { label: 'Masters Degree', value: EducationV2.MASTER },
              { label: 'Doctorate', value: EducationV2.DOCTORATE },
              {
                label: 'Prefer not to answer',
                value: EducationV2.PREFER_NOT_TO_ANSWER,
              },
            ]}
            selected={survey.education}
            onChange={(value) => onUpdate(EDUCATION, value)}
          />

          <YesNoOptionalQuestion
            question={disadvantagedBackgroundQuestion}
            selected={survey.disadvantaged}
            onChange={(value) => onUpdate(DISADVANTAGED, value)}
          />

          <FlexColumn style={{ marginTop: '1.5rem' }}>
            <div
              style={{ ...styles.question, flex: 1, marginBottom: '0.375rem' }}
            >
              {getQuestionLabel(
                DEMOGRAPHIC_SURVEY_COMMENTS,
                'Is there any aspect of your identity that we have not covered' +
                  ' in the preceding questions that we may want to consider including' +
                  ' in future surveys?'
              )}
            </div>
            <TextAreaWithLengthValidationMessage
              id='survey-comments'
              onChange={(value) => onUpdate('surveyComments', value)}
              initialText={survey.surveyComments || ''}
              textBoxStyleOverrides={{ width: '100%' }}
              maxCharacters={SURVEY_COMMENTS_MAX_LENGTH}
            />
          </FlexColumn>
        </FlexColumn>
      </FlexColumn>
    );
  }
);
