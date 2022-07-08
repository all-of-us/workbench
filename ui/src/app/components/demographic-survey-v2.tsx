import * as React from 'react';
import { useEffect, useRef, useState } from 'react';
import * as fp from 'lodash/fp';
import validate from 'validate.js';

import {
  DemographicSurveyV2,
  EducationV2,
  EthnicCategory,
  GenderIdentityV2,
  Profile,
  SexAtBirthV2,
  SexualOrientationV2,
} from 'generated/fetch';

import { FlexColumn, FlexRow } from 'app/components/flex';
import { withProfileErrorModal } from 'app/components/with-error-modal';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import { reactStyles } from 'app/utils';

import { CheckBox, NumberInput, TextInput } from './inputs';
import { MultipleChoiceQuestion } from './multiple-choice-question';
import { AoU } from './text-wrappers';
import { WithSpinnerOverlayProps } from './with-spinner-overlay';
import { YesNoOptionalQuestion } from './yes-no-optional-question';

const maxYear = new Date().getFullYear();
const minYear = maxYear - 125;
const styles = reactStyles({
  question: { fontSize: 18, fontWeight: 'bold', color: colors.primary },
  answer: { margin: '0.0rem 0.25rem', color: colors.primary },
});

const NONE_FULLY_DESCRIBE =
  'None of these fully describe me, and I want to specify';
const TWO_SPIRIT_DISABLED_TEXT =
  'Two Spirit is an identity unique to people of American Indian and Alaska Native ' +
  'ancestry. If this applies to you, please update your selection in the ' +
  '"Race and Ethnicities" section.';

const validateDemographicSurvey = (demographicSurvey: DemographicSurveyV2) => {
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
              '"Prefer not to answer" checkbox',
          },
          numericality: {
            onlyInteger: true,
            greaterThanOrEqualTo: minYear,
            lessThanOrEqualTo: maxYear,
          },
        },
      };

  const ethnicityAiAnOtherText = demographicSurvey.ethnicCategories.some(
    (ec) => ec === EthnicCategory.AIANOTHER
  )
    ? {
        ethnicityAiAnOtherText: {
          presence: {
            allowEmpty: false,
            message:
              '^If selecting American Indian or Alaska Native (AIAN) ' +
              '"None of these fully describe me, and I want to specify", please specify a value',
          },
        },
      }
    : {};

  const ethnicityAsianOtherText = demographicSurvey.ethnicCategories.some(
    (ec) => ec === EthnicCategory.ASIANOTHER
  )
    ? {
        ethnicityAsianOtherText: {
          presence: {
            allowEmpty: false,
            message:
              '^If selecting Asian "None of these fully describe me, and I want to specify", ' +
              'please specify a value',
          },
        },
      }
    : {};

  const ethnicityBlackOtherText = demographicSurvey.ethnicCategories.some(
    (ec) => ec === EthnicCategory.BLACKOTHER
  )
    ? {
        ethnicityBlackOtherText: {
          presence: {
            allowEmpty: false,
            message:
              '^If selecting Black or African American "None of these fully describe me, ' +
              'and I want to specify", please specify a value',
          },
        },
      }
    : {};

  const ethnicityHispanicOtherText = demographicSurvey.ethnicCategories.some(
    (ec) => ec === EthnicCategory.HISPANICOTHER
  )
    ? {
        ethnicityHispanicOtherText: {
          presence: {
            allowEmpty: false,
            message:
              '^If selecting Hispanic or Latino or Spanish Origin ' +
              '"None of these fully describe me, and I want to specify", please specify a value',
          },
        },
      }
    : {};

  const ethnicityMeNaOtherText = demographicSurvey.ethnicCategories.some(
    (ec) => ec === EthnicCategory.MENAOTHER
  )
    ? {
        ethnicityMeNaOtherText: {
          presence: {
            allowEmpty: false,
            message:
              '^If selecting Middle Eastern or North African ' +
              '"None of these fully describe me, and I want to specify", please specify a value',
          },
        },
      }
    : {};

  const ethnicityNhPiOtherText = demographicSurvey.ethnicCategories.some(
    (ec) => ec === EthnicCategory.NHPIOTHER
  )
    ? {
        ethnicityNhPiOtherText: {
          presence: {
            allowEmpty: false,
            message:
              '^If selecting Native Hawaiian or Other Pacific Islander ' +
              '"None of these fully describe me, and I want to specify", please specify a value',
          },
        },
      }
    : {};

  const ethnicityWhiteOtherText = demographicSurvey.ethnicCategories.some(
    (ec) => ec === EthnicCategory.WHITEOTHER
  )
    ? {
        ethnicityWhiteOtherText: {
          presence: {
            allowEmpty: false,
            message:
              '^If selecting White "None of these fully describe me, and I want to specify", ' +
              'please specify a value',
          },
        },
      }
    : {};

  const ethnicityOtherText = demographicSurvey.ethnicCategories.some(
    (ec) => ec === EthnicCategory.OTHER
  )
    ? {
        ethnicityOtherText: {
          presence: {
            allowEmpty: false,
            message:
              '^If selecting "None of these fully describe me, ' +
              'and I want to specify" for your Race(s) and/or Ethnicities, please specify a value',
          },
        },
      }
    : {};

  const genderOtherText = demographicSurvey.genderIdentities.some(
    (ec) => ec === GenderIdentityV2.OTHER
  )
    ? {
        genderOtherText: {
          presence: {
            allowEmpty: false,
            message:
              '^If selecting "None of these fully describe me, and I want to specify" for your ' +
              'Gender Identity/Identities, please specify a value',
          },
        },
      }
    : {};

  const sexAtBirthOtherText =
    demographicSurvey.sexAtBirth === SexAtBirthV2.OTHER
      ? {
          sexAtBirthOtherText: {
            presence: {
              allowEmpty: false,
              message:
                '^If selecting "None of these fully describe me, and I want to specify" ' +
                'for your Sex Assigned at Birth, please specify a value',
            },
          },
        }
      : {};

  const orientationOtherText = demographicSurvey.sexualOrientations.some(
    (ec) => ec === SexualOrientationV2.OTHER
  )
    ? {
        orientationOtherText: {
          presence: {
            allowEmpty: false,
            message:
              '^If selecting "None of these fully describe me, and I want to specify" ' +
              'for your Sexual Orientation(s), please specify a value',
          },
        },
      }
    : {};

  // TODO Look into if this can be done more efficiently
  const validationCheck = {
    ethnicCategories: { presence: { allowEmpty: false } },
    genderIdentities: { presence: { allowEmpty: false } },
    sexAtBirth: { presence: { allowEmpty: false } },
    ...yearOfBirth,
    education: { presence: { allowEmpty: false } },
    disadvantaged: { presence: { allowEmpty: false } },
    disabilityHearing: { presence: { allowEmpty: false } },
    disabilitySeeing: { presence: { allowEmpty: false } },
    disabilityConcentrating: { presence: { allowEmpty: false } },
    disabilityWalking: { presence: { allowEmpty: false } },
    disabilityDressing: { presence: { allowEmpty: false } },
    disabilityErrands: { presence: { allowEmpty: false } },
    sexualOrientations: { presence: { allowEmpty: false } },
    ...ethnicityAiAnOtherText,
    ...ethnicityAsianOtherText,
    ...ethnicityBlackOtherText,
    ...ethnicityHispanicOtherText,
    ...ethnicityMeNaOtherText,
    ...ethnicityNhPiOtherText,
    ...ethnicityWhiteOtherText,
    ...ethnicityOtherText,
    ...genderOtherText,
    ...sexAtBirthOtherText,
    ...orientationOtherText,
  };

  return validate(demographicSurvey, validationCheck);
};

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
      survey.ethnicCategories.some((s) => s === EthnicCategory.AIAN)
    );
    const [showAsianOptions, setShowAsianOptions] = useState(
      survey.ethnicCategories.some((s) => s === EthnicCategory.ASIAN)
    );
    const [showAiAnOptions, setShowAiAnOptions] = useState(
      survey.ethnicCategories.some((s) => s === EthnicCategory.AIAN)
    );
    const [showBlackOptions, setShowBlackOptions] = useState(
      survey.ethnicCategories.some((s) => s === EthnicCategory.BLACK)
    );
    const [showHispanicOptions, setShowHispanicOptions] = useState(
      survey.ethnicCategories.some((s) => s === EthnicCategory.HISPANIC)
    );
    const [showMeNaOptions, setShowMeNaOptions] = useState(
      survey.ethnicCategories.some((s) => s === EthnicCategory.MENA)
    );
    const [showNhPiOptions, setShowNhPiOptions] = useState(
      survey.ethnicCategories.some((s) => s === EthnicCategory.NHPI)
    );
    const [showWhiteOptions, setShowWhiteOptions] = useState(
      survey.ethnicCategories.some((s) => s === EthnicCategory.WHITE)
    );
    const raceEthnicityRef = useRef(null);

    useEffect(() => {
      onError(validateDemographicSurvey(survey));
    }, [profile]);

    useEffect(() => {
      setIsAian(survey.ethnicCategories.some((s) => s === EthnicCategory.AIAN));
    }, [survey.ethnicCategories]);

    const disadvantagedBackgroundQuestion = (
      <div>
        14. Are you an individual from a disadvantaged background, as &nbsp;
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
          style={{
            backgroundColor: colorWithWhiteness(colors.accent, 0.75),
            padding: '1rem',
            borderRadius: '5px',
            color: colors.primary,
            marginBottom: '1rem',
            marginTop: '1rem',
          }}
        >
          <div>
            The <AoU /> Research Program is dedicated to cultivating a diverse
            research community and building an inclusive platform. Your answers
            to these questions will help us learn more about who is using the
            research platform.
          </div>
          <div style={{ marginTop: '1rem' }}>
            Your privacy is important to us. Your individual responses will not
            be displayed on your researcher profile and will only be accessible
            to a limited number of authorized <AoU /> personnel. <AoU /> may
            externally report de-identified, aggregate data on the demographics
            of individuals using the Researcher Workbench.
          </div>
        </div>
        <FlexColumn>
          <MultipleChoiceQuestion
            question='1. Which Racial and Ethnic categories describe you?'
            label='Racial and/or Ethnic Identity/Identities'
            options={[
              {
                label: 'American Indian or Alaska Native (AIAN)',
                value: EthnicCategory.AIAN,
                subOptions: [
                  {
                    label: 'American Indian',
                    value: EthnicCategory.AIANAMERICANINDIAN,
                  },
                  {
                    label: 'Alaska Native',
                    value: EthnicCategory.AIANALASKANATIVE,
                  },
                  {
                    label: 'Central or South American Indian',
                    value: EthnicCategory.AIANCENTRALSOUTH,
                  },
                  {
                    label: NONE_FULLY_DESCRIBE,
                    value: EthnicCategory.AIANOTHER,
                    showInput: true,
                    otherText: survey.ethnicityAiAnOtherText,
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
                onChange: (value) => setShowAiAnOptions(value),
                onExpand: () => setShowAiAnOptions(!showAiAnOptions),
              },

              {
                label: 'Asian',
                value: EthnicCategory.ASIAN,
                subOptions: [
                  { label: 'Asian Indian', value: EthnicCategory.ASIANINDIAN },
                  { label: 'Cambodian', value: EthnicCategory.ASIANCAMBODIAN },
                  { label: 'Chinese', value: EthnicCategory.ASIANCHINESE },
                  { label: 'Filipino', value: EthnicCategory.ASIANFILIPINO },
                  { label: 'Hmong', value: EthnicCategory.ASIANHMONG },
                  { label: 'Japanese', value: EthnicCategory.ASIANJAPANESE },
                  { label: 'Korean', value: EthnicCategory.ASIANKOREAN },
                  { label: 'Lao', value: EthnicCategory.ASIANLAO },
                  { label: 'Pakistani', value: EthnicCategory.ASIANPAKISTANI },
                  {
                    label: 'Vietnamese',
                    value: EthnicCategory.ASIANVIETNAMESE,
                  },
                  {
                    label:
                      'None of these fully describe me, and I want to specify.',
                    value: EthnicCategory.ASIANOTHER,
                    showInput: true,
                    otherText: survey.ethnicityAsianOtherText,
                    otherTextMaxLength: 200,
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
                onChange: (value) => setShowAsianOptions(value),
                onExpand: () => setShowAsianOptions(!showAsianOptions),
              },
              {
                label: 'Black or African American',
                value: EthnicCategory.BLACK,
                subOptions: [
                  {
                    label: 'African American',
                    value: EthnicCategory.BLACKAA,
                  },
                  { label: 'Barbadian', value: EthnicCategory.BLACKBARBADIAN },
                  { label: 'Caribbean', value: EthnicCategory.BLACKCARIBBEAN },
                  { label: 'Ethiopian', value: EthnicCategory.BLACKETHIOPIAN },
                  { label: 'Ghanaian', value: EthnicCategory.BLACKGHANAIAN },
                  { label: 'Haitian', value: EthnicCategory.BLACKHAITIAN },
                  { label: 'Jamaican', value: EthnicCategory.BLACKJAMAICAN },
                  { label: 'Liberian', value: EthnicCategory.BLACKLIBERIAN },
                  { label: 'Nigerian', value: EthnicCategory.BLACKNIGERIAN },
                  { label: 'Somali', value: EthnicCategory.BLACKSOMALI },
                  {
                    label: 'South African',
                    value: EthnicCategory.BLACKSOUTHAFRICAN,
                  },
                  {
                    label: NONE_FULLY_DESCRIBE,
                    value: EthnicCategory.BLACKOTHER,
                    showInput: true,
                    otherText: survey.ethnicityBlackOtherText,
                    otherTextMaxLength: 200,
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
                onChange: (value) => setShowBlackOptions(value),
                onExpand: () => setShowBlackOptions(!showBlackOptions),
              },
              {
                label: 'Hispanic or Latino or Spanish Origin',
                value: EthnicCategory.HISPANIC,
                subOptions: [
                  {
                    label: 'Columbian',
                    value: EthnicCategory.HISPANICCOLUMBIAN,
                  },
                  { label: 'Cuban', value: EthnicCategory.HISPANICCUBAN },
                  {
                    label: 'Dominican',
                    value: EthnicCategory.HISPANICDOMINICAN,
                  },
                  {
                    label: 'Ecuadorian',
                    value: EthnicCategory.HISPANICECUADORIAN,
                  },
                  { label: 'Honduran', value: EthnicCategory.HISPANICHONDURAN },
                  {
                    label: 'Mexican or Mexican American',
                    value: EthnicCategory.HISPANICMEXICAN,
                  },
                  {
                    label: 'Puerto Rican',
                    value: EthnicCategory.HISPANICPUERTORICAN,
                  },
                  {
                    label: 'Salvadoran',
                    value: EthnicCategory.HISPANICSALVADORAN,
                  },
                  { label: 'Spanish', value: EthnicCategory.HISPANICSPANISH },
                  {
                    label: NONE_FULLY_DESCRIBE,
                    value: EthnicCategory.HISPANICOTHER,
                    showInput: true,
                    otherText: survey.ethnicityHispanicOtherText,
                    otherTextMaxLength: 200,
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
                onChange: (value) => setShowHispanicOptions(value),
                onExpand: () => setShowHispanicOptions(!showHispanicOptions),
              },
              {
                label: 'Middle Eastern or North African',
                value: EthnicCategory.MENA,
                subOptions: [
                  {
                    label: 'Afghan',
                    value: EthnicCategory.MENAAFGHAN,
                  },
                  { label: 'Algerian', value: EthnicCategory.MENAALGERIAN },
                  { label: 'Egyptian', value: EthnicCategory.MENAEGYPTIAN },
                  {
                    label: 'Iranian',
                    value: EthnicCategory.MENAIRANIAN,
                  },
                  { label: 'Iraqi', value: EthnicCategory.MENAIRAQI },
                  { label: 'Israeli', value: EthnicCategory.MENAISRAELI },
                  {
                    label: 'Lebanese',
                    value: EthnicCategory.MENALEBANESE,
                  },
                  {
                    label: 'Moroccan',
                    value: EthnicCategory.MENAMOROCCAN,
                  },
                  { label: 'Syrian', value: EthnicCategory.MENASYRIAN },
                  { label: 'Tunisian', value: EthnicCategory.MENATUNISIAN },
                  {
                    label: NONE_FULLY_DESCRIBE,
                    value: EthnicCategory.MENAOTHER,
                    showInput: true,
                    otherText: survey.ethnicityMeNaOtherText,
                    otherTextMaxLength: 200,
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
                onChange: (value) => setShowMeNaOptions(value),
                onExpand: () => setShowMeNaOptions(!showMeNaOptions),
              },
              {
                label: 'Native Hawaiian or Other Pacific Islander',
                value: EthnicCategory.NHPI,
                subOptions: [
                  {
                    label: 'Chamorro',
                    value: EthnicCategory.NHPICHAMORRO,
                  },
                  { label: 'Chuukese', value: EthnicCategory.NHPICHUUKESE },
                  { label: 'Fijian', value: EthnicCategory.NHPIFIJIAN },
                  {
                    label: 'Marshallese',
                    value: EthnicCategory.NHPIMARSHALLESE,
                  },
                  {
                    label: 'Native Hawaiian',
                    value: EthnicCategory.NHPIHAWAIIAN,
                  },
                  { label: 'Palauan', value: EthnicCategory.NHPIPALAUAN },
                  {
                    label: 'Samoan',
                    value: EthnicCategory.NHPISAMOAN,
                  },
                  {
                    label: 'Tahitian',
                    value: EthnicCategory.NHPITAHITIAN,
                  },
                  { label: 'Tongan', value: EthnicCategory.NHPITONGAN },
                  {
                    label: NONE_FULLY_DESCRIBE,
                    value: EthnicCategory.NHPIOTHER,
                    showInput: true,
                    otherText: survey.ethnicityNhPiOtherText,
                    otherTextMaxLength: 200,
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
                onChange: (value) => setShowNhPiOptions(value),
                onExpand: () => setShowNhPiOptions(!showNhPiOptions),
              },
              {
                label: 'White',
                value: EthnicCategory.WHITE,
                subOptions: [
                  {
                    label: 'Dutch',
                    value: EthnicCategory.WHITEDUTCH,
                  },
                  { label: 'English', value: EthnicCategory.WHITEENGLISH },
                  { label: 'European', value: EthnicCategory.WHITEEUROPEAN },
                  {
                    label: 'French',
                    value: EthnicCategory.WHITEFRENCH,
                  },
                  {
                    label: 'German',
                    value: EthnicCategory.WHITEGERMAN,
                  },
                  { label: 'Irish', value: EthnicCategory.WHITEIRISH },
                  {
                    label: 'Italian',
                    value: EthnicCategory.WHITEITALIAN,
                  },
                  {
                    label: 'Norwegian',
                    value: EthnicCategory.WHITENORWEGIAN,
                  },
                  { label: 'Polish', value: EthnicCategory.WHITEPOLISH },
                  { label: 'Scottish', value: EthnicCategory.WHITESCOTTISH },
                  { label: 'Spanish', value: EthnicCategory.WHITESPANISH },
                  {
                    label: NONE_FULLY_DESCRIBE,
                    value: EthnicCategory.WHITEOTHER,
                    showInput: true,
                    otherText: survey.ethnicityWhiteOtherText,
                    otherTextMaxLength: 200,
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
                onChange: (value) => setShowWhiteOptions(value),
                onExpand: () => {
                  setShowWhiteOptions(!showWhiteOptions);
                },
              },
              {
                label: NONE_FULLY_DESCRIBE,
                value: EthnicCategory.OTHER,
                showInput: true,
                otherText: survey.ethnicityOtherText,
                otherTextMaxLength: 200,
                onChange: (value) => {
                  if (!value) {
                    onUpdate('ethnicityOtherText', null);
                  }
                },
                onChangeOtherText: (value) =>
                  onUpdate('ethnicityOtherText', value),
              },
              {
                label: 'Prefer not to answer',
                value: EthnicCategory.PREFERNOTTOANSWER,
                onChange: (checked) => {
                  onUpdate(
                    'ethnicCategories',
                    checked ? [EthnicCategory.PREFERNOTTOANSWER] : []
                  );
                  setShowAiAnOptions(!checked);
                  setShowAsianOptions(!checked);
                  setShowBlackOptions(!checked);
                  setShowHispanicOptions(!checked);
                  setShowMeNaOptions(!checked);
                  setShowNhPiOptions(!checked);
                  setShowWhiteOptions(!checked);
                  raceEthnicityRef.current.scrollIntoView();
                },
              },
            ]}
            multiple
            selected={survey.ethnicCategories}
            onChange={(value) =>
              onUpdate(
                'ethnicCategories',
                value.filter((v) => v !== EthnicCategory.PREFERNOTTOANSWER)
              )
            }
            style={{ marginBottom: '1rem' }}
            refProp={raceEthnicityRef}
          />
          <MultipleChoiceQuestion
            question='2. What terms best express how you describe your current gender identity?'
            label='Gender Identity/Identities'
            options={[
              { label: 'Genderqueer', value: GenderIdentityV2.GENDERQUEER },
              { label: 'Man', value: GenderIdentityV2.MAN },
              { label: 'Non-binary', value: GenderIdentityV2.NONBINARY },
              {
                label: 'Trans man/Transgender man',
                value: GenderIdentityV2.TRANSMAN,
              },
              {
                label: 'Trans woman/Transgender woman',
                value: GenderIdentityV2.TRANSWOMAN,
              },
              {
                label: 'Two Spirit',
                value: GenderIdentityV2.TWOSPIRIT,
                disabled: !isAian,
                disabledText: TWO_SPIRIT_DISABLED_TEXT,
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
                value: GenderIdentityV2.PREFERNOTTOANSWER,
                onChange: (checked) =>
                  onUpdate(
                    'genderIdentities',
                    checked ? [GenderIdentityV2.PREFERNOTTOANSWER] : []
                  ),
              },
              {
                label: NONE_FULLY_DESCRIBE,
                value: GenderIdentityV2.OTHER,
                showInput: true,
                otherText: survey.genderOtherText,
                otherTextMaxLength: 200,
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
                'genderIdentities',
                value.filter((v) => v !== GenderIdentityV2.PREFERNOTTOANSWER)
              )
            }
            style={{ marginBottom: '1rem' }}
          />
          <MultipleChoiceQuestion
            question='3. What was the sex assigned to you at birth, such as on your original birth certificate?'
            label='Sex Assigned at Birth'
            options={[
              { label: 'Female', value: SexAtBirthV2.FEMALE },
              { label: 'Intersex', value: SexAtBirthV2.INTERSEX },
              { label: 'Male', value: SexAtBirthV2.MALE },
              {
                label: 'Prefer not to answer',
                value: SexAtBirthV2.PREFERNOTTOANSWER,
              },
              {
                label: NONE_FULLY_DESCRIBE,
                value: SexAtBirthV2.OTHER,
                showInput: true,
                otherText: survey.sexAtBirthOtherText,
                otherTextMaxLength: 200,
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
              onUpdate('sexAtBirth', value);
            }}
            style={{ marginBottom: '1rem' }}
          />
          <MultipleChoiceQuestion
            question='4. What terms best express how you describe your current sexual orientation?'
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
                value: SexualOrientationV2.SAMEGENDER,
              },
              {
                label: 'Straight or heterosexual',
                value: SexualOrientationV2.STRAIGHT,
              },
              {
                label: 'Two Spirit',
                value: SexualOrientationV2.TWOSPIRIT,
                disabled: !isAian,
                disabledText: TWO_SPIRIT_DISABLED_TEXT,
              },
              {
                label: 'Questioning or unsure of my sexual orientation',
                value: SexualOrientationV2.QUESTIONING,
              },
              {
                label: NONE_FULLY_DESCRIBE,
                value: SexualOrientationV2.OTHER,
                showInput: true,
                otherText: survey.orientationOtherText,
                otherTextMaxLength: 200,
                onChange: (value) => {
                  if (!value) {
                    onUpdate('orientationOtherText', null);
                  }
                },
                onChangeOtherText: (value) =>
                  onUpdate('orientationOtherText', value),
              },
              {
                label: 'Prefer not to answer',
                value: SexualOrientationV2.PREFERNOTTOANSWER,
                onChange: (checked) =>
                  onUpdate(
                    'sexualOrientations',
                    checked ? [SexualOrientationV2.PREFERNOTTOANSWER] : []
                  ),
              },
            ]}
            multiple
            selected={survey.sexualOrientations}
            onChange={(value) =>
              onUpdate(
                'sexualOrientations',
                value.filter((v) => v !== SexualOrientationV2.PREFERNOTTOANSWER)
              )
            }
            style={{ marginBottom: '1rem' }}
          />
          <YesNoOptionalQuestion
            question='5. Are you deaf or do you have serious difficulty hearing?'
            selected={survey.disabilityHearing}
            onChange={(value) => onUpdate('disabilityHearing', value)}
            style={{ marginBottom: '1rem' }}
          />
          <YesNoOptionalQuestion
            question='6. Are you blind or do you have serious difficulty seeing, even when
            wearing glasses?'
            selected={survey.disabilitySeeing}
            onChange={(value) => onUpdate('disabilitySeeing', value)}
            style={{ marginBottom: '1rem' }}
          />
          <YesNoOptionalQuestion
            question='7. Because of a physical, cognitive, or emotional condition, do you
            have serious difficulty concentrating, remembering, or making
            decisions?'
            selected={survey.disabilityConcentrating}
            onChange={(value) => onUpdate('disabilityConcentrating', value)}
            style={{ marginBottom: '1rem' }}
          />
          <YesNoOptionalQuestion
            question='8. Do you have serious difficulty walking or climbing stairs?'
            selected={survey.disabilityWalking}
            onChange={(value) => onUpdate('disabilityWalking', value)}
            style={{ marginBottom: '1rem' }}
          />
          <YesNoOptionalQuestion
            question='9. Do you have difficulty dressing or bathing?'
            selected={survey.disabilityDressing}
            onChange={(value) => onUpdate('disabilityDressing', value)}
            style={{ marginBottom: '1rem' }}
          />
          <YesNoOptionalQuestion
            question="10. Because of a physical, mental, or emotional condition, do you have
            difficulty doing errands alone such as visiting doctor's office or shopping?"
            selected={survey.disabilityErrands}
            onChange={(value) => onUpdate('disabilityErrands', value)}
            style={{ marginBottom: '1rem' }}
          />
          <FlexColumn style={{ marginBottom: '1rem' }}>
            <div
              style={{ ...styles.question, flex: 1, marginBottom: '0.25rem' }}
            >
              11. Do you have a physical, cognitive, and/or emotional condition
              that substantially limits one or more life activities not
              specified through the above questions, and want to share more?
              Please describe.
            </div>
            <TextInput
              onChange={(value) => onUpdate('disabilityOtherText', value)}
              value={survey.disabilityOtherText || ''}
              style={{ width: '50%' }}
            />
          </FlexColumn>
          <div style={styles.question}>12. Year of birth</div>
          <FlexRow style={{ alignItems: 'center', marginBottom: '1rem' }}>
            <NumberInput
              onChange={(value) => onUpdate('yearOfBirth', value)}
              disabled={survey.yearOfBirthPreferNot}
              min={minYear}
              max={maxYear}
              value={survey.yearOfBirth || ''}
              style={{ width: '4rem' }}
            />
            <CheckBox
              checked={survey.yearOfBirthPreferNot}
              onChange={(value) => {
                onUpdate('yearOfBirth', null);
                onUpdate('yearOfBirthPreferNot', value);
              }}
              style={{ marginLeft: '1rem' }}
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
            question={'13. Highest Level of Education'}
            label='Highest Level of Education Completed'
            options={[
              {
                label: 'Never attended school/no formal education',
                value: EducationV2.NOEDUCATION,
              },
              {
                label:
                  'Primary/Middle School/High School (Grades 1 through 12/GED)',
                value: EducationV2.GRADES112,
              },
              {
                label:
                  'Some college, Associate Degree or Technical School (1 to 3 years) or current undergraduate student',
                value: EducationV2.UNDERGRADUATE,
              },
              {
                label:
                  'College graduate (4 years or more) or current post-graduate traineee',
                value: EducationV2.COLLEGEGRADUATE,
              },

              { label: 'Masters Degree', value: EducationV2.MASTER },
              { label: 'Doctorate', value: EducationV2.DOCTORATE },
              {
                label: 'Prefer not to answer',
                value: EducationV2.PREFERNOTTOANSWER,
              },
            ]}
            selected={survey.education}
            onChange={(value) => onUpdate('education', value)}
            style={{ marginBottom: '1rem' }}
          />

          <YesNoOptionalQuestion
            question={disadvantagedBackgroundQuestion}
            selected={survey.disadvantaged}
            onChange={(value) => onUpdate('disadvantaged', value)}
          />

          <FlexColumn style={{ marginTop: '1rem' }}>
            <div
              style={{ ...styles.question, flex: 1, marginBottom: '0.25rem' }}
            >
              15. Is there any aspect of your identity that we have not covered
              in the preceding questions that we may want to consider including
              in future surveys?
            </div>
            <TextInput
              onChange={(value) => onUpdate('surveyComments', value)}
              value={survey.surveyComments || ''}
              style={{ width: '50%' }}
            />
          </FlexColumn>
        </FlexColumn>
      </FlexColumn>
    );
  }
);
