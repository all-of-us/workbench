import * as React from 'react';
import { useEffect, useState } from 'react';
import * as fp from 'lodash/fp';
import validate from 'validate.js';

import {
  EducationV2,
  EthnicCategory,
  GenderIdentityV2,
  Profile,
  SexAtBirthV2,
  SexualOrientationV2,
} from 'generated/fetch';

import { FlexColumn, FlexRow } from 'app/components/flex';
import { SemiBoldHeader } from 'app/components/headers';
import { withProfileErrorModal } from 'app/components/with-error-modal';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import { reactStyles } from 'app/utils';

import { Divider } from './divider';
import { CheckBox, NumberInput, TextInput } from './inputs';
import { MultipleChoiceQuestion } from './multiple-choice-question';
import { AoU } from './text-wrappers';
import { WithSpinnerOverlayProps } from './with-spinner-overlay';
import { YesNoOptionalQuestion } from './yes-no-optional-question';

const maxYear = new Date().getFullYear();
const minYear = maxYear - 125;
const styles = reactStyles({
  question: { fontWeight: 'bold', color: colors.primary },
  answer: { margin: '0.0rem 0.25rem', color: colors.primary },
});

const validateDemographicSurvey = (demographicSurvey) => {
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
              '^You must either fill in your year of birth or check the corresponding "Prefer not to answer" checkbox',
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
    ...yearOfBirth,
    education: { presence: { allowEmpty: false } },
    disadvantaged: { presence: { allowEmpty: false } },
  };
  return validate(demographicSurvey, validationCheck);
};

interface DemographicSurveyProps extends WithSpinnerOverlayProps {
  onError: (any) => void;
  onUpdate: (string, any) => void;
  profile: Profile;
}

const DemographicSurvey = fp.flow(withProfileErrorModal)(
  (props: DemographicSurveyProps) => {
    const [isAian, setIsAian] = useState(false);
    const [showAsianOptions, setShowAsianOptions] = useState(false);
    const [showAiAnOptions, setShowAiAnOptions] = useState(false);
    const [showBlackOptions, setShowBlackOptions] = useState(false);
    const [showHispanicOptions, setShowHispanicOptions] = useState(false);
    const [showMeNaOptions, setShowMeNaOptions] = useState(false);
    const [showNhPiOptions, setShowNhPiOptions] = useState(false);
    const [showWhiteOptions, setShowWhiteOptions] = useState(false);

    const { onError, onUpdate, profile } = props;
    const { demographicSurveyV2: survey } = profile;

    useEffect(() => {
      onError(validateDemographicSurvey(survey));
    }, [profile]);

    useEffect(() => {
      setIsAian(
        survey.ethnicCategories.filter(
          (s) =>
            s === EthnicCategory.AIAN ||
            s === EthnicCategory.AIANCENTRALSOUTH ||
            s === EthnicCategory.AIANOTHER
        ).length > 0
      );
    }, [survey.ethnicCategories]);

    const disadvantagedBackgroundQuestion = (
      <div>
        14. Are you an individual from a disadvantaged background, as defined by
        &nbsp;
        <a
          target='_blank'
          href='https://extramural-diversity.nih.gov/diversity-matters/disadvantaged-backgrounds'
        >
          NIH Diversity in Extramural Programs?
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
          <SemiBoldHeader>Races and Ethnicities</SemiBoldHeader>
          <Divider style={{ marginTop: '.25rem' }} />
          <MultipleChoiceQuestion
            question='1. Which races and/or ethnicities do you identify with? Please select all that apply.'
            options={[
              {
                label: 'American Indian or Alaska Native',
                value: EthnicCategory.AIAN,
                subOptions: [
                  {
                    label: 'Central or South American Indian',
                    value: EthnicCategory.AIANCENTRALSOUTH,
                  },
                  {
                    label:
                      'None of these fully describe me, and I want to specify',
                    value: EthnicCategory.AIANOTHER,
                    showInput: true,
                    otherText: survey.ethnicityAiAnOtherText,
                    onChange: (checked) => {
                      if (!checked) {
                        onUpdate('ethnicityAiAnOtherText', null);
                      }
                    },
                    onChangeOtherText: (value) => {
                      onUpdate('ethnicityAiAnOtherText', value);
                    },
                  },
                ],
                showSubOptions: showAiAnOptions,
                onChange: (value) => {
                  setShowAiAnOptions(value);
                },
                onExpand: () => {
                  setShowAiAnOptions(!showAiAnOptions);
                },
              },

              {
                label: 'Asian',
                value: EthnicCategory.ASIAN,
                subOptions: [
                  { label: 'Cambodian', value: EthnicCategory.ASIANCAMBODIAN },
                  { label: 'Chinese', value: EthnicCategory.ASIANCHINESE },
                  { label: 'Filipino', value: EthnicCategory.ASIANFILIPINO },
                  { label: 'Hmong', value: EthnicCategory.ASIANHMONG },
                  { label: 'Indian', value: EthnicCategory.ASIANINDIAN },
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
                onChange: (value) => {
                  setShowAsianOptions(value);
                },
                onExpand: () => {
                  setShowAsianOptions(!showAsianOptions);
                },
              },
              {
                label: 'Black, African American, or of African descent',
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
                    label:
                      'None of these fully describe me, and I want to specify',
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
                onChange: (value) => {
                  setShowBlackOptions(value);
                },
                onExpand: () => {
                  setShowBlackOptions(!showBlackOptions);
                },
              },
              {
                label: 'Hispanic, Latino, or Spanish descent',
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
                  { label: 'Mexican', value: EthnicCategory.HISPANICMEXICAN },
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
                    label:
                      'None of these fully describe me, and I want to specify',
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
                onChange: (value) => {
                  setShowHispanicOptions(value);
                },
                onExpand: () => {
                  setShowHispanicOptions(!showHispanicOptions);
                },
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
                    label:
                      'None of these fully describe me, and I want to specify',
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
                onChange: (value) => {
                  setShowMeNaOptions(value);
                },
                onExpand: () => {
                  setShowMeNaOptions(!showMeNaOptions);
                },
              },
              {
                label: 'Native Hawaiian or other Pacific Islander',
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
                    label:
                      'None of these fully describe me, and I want to specify',
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
                onChange: (value) => {
                  setShowNhPiOptions(value);
                },
                onExpand: () => {
                  setShowNhPiOptions(!showNhPiOptions);
                },
              },
              {
                label: 'White, or of European descent',
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
                    label:
                      'None of these fully describe me, and I want to specify',
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
                onChange: (value) => {
                  setShowWhiteOptions(value);
                },
                onExpand: () => {
                  setShowWhiteOptions(!showWhiteOptions);
                },
              },
              {
                label: 'None of these fully describe me, and I want to specify',
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
                onChange: (checked) =>
                  onUpdate(
                    'ethnicCategories',
                    checked ? [EthnicCategory.PREFERNOTTOANSWER] : []
                  ),
              },
            ]}
            multiple
            selected={survey.ethnicCategories}
            onChange={(value) => {
              onUpdate(
                'ethnicCategories',
                value.length > 1
                  ? value.filter((v) => v !== EthnicCategory.PREFERNOTTOANSWER)
                  : value
              );
            }}
            style={{ marginBottom: '1rem' }}
          />
          <SemiBoldHeader>Questions about gender</SemiBoldHeader>
          <Divider style={{ marginTop: '.25rem' }} />
          <MultipleChoiceQuestion
            question='2. What terms best express how you describe your current gender identity?'
            options={[
              { label: 'Gender Queer', value: GenderIdentityV2.GENDERQUEER },
              { label: 'Man', value: GenderIdentityV2.MAN },
              { label: 'Non-binary', value: GenderIdentityV2.NONBINARY },
              {
                label: 'Questioning or unsure of my gender identity',
                value: GenderIdentityV2.QUESTIONING,
              },
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
                disabledText:
                  'Two Spirit is an identity unique to people of American Indian and Alaska Native ' +
                  'ancestry. If this applies to you, please update your selection in the ' +
                  '"Race and Ethnicities" section.',
              },
              {
                label: 'Woman',
                value: GenderIdentityV2.WOMAN,
              },
              {
                label: 'None of these fully describe me, and I want to specify',
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
              {
                label: 'Prefer not to answer',
                value: GenderIdentityV2.PREFERNOTTOANSWER,
                onChange: (checked) =>
                  onUpdate(
                    'genderIdentities',
                    checked ? [GenderIdentityV2.PREFERNOTTOANSWER] : []
                  ),
              },
            ]}
            multiple
            selected={survey.genderIdentities}
            onChange={(value) => {
              onUpdate(
                'genderIdentities',
                value.length > 1
                  ? value.filter(
                      (v) => v !== GenderIdentityV2.PREFERNOTTOANSWER
                    )
                  : value
              );
            }}
            style={{ marginBottom: '1rem' }}
          />
          <MultipleChoiceQuestion
            question='3. What terms best express how you describe your current sexual orientation?'
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
                label: 'Questioning or unsure of my sexual orientation',
                value: SexualOrientationV2.QUESTIONING,
              },
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
                disabledText:
                  'Two Spirit is an identity unique to people of American Indian and Alaska Native ' +
                  'ancestry. If this applies to you, please update your selection in the ' +
                  '"Race and Ethnicities" section.',
              },
              {
                label: 'None of these fully describe me, and I want to specify',
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
            onChange={(value) => {
              onUpdate(
                'sexualOrientations',
                value.length > 1
                  ? value.filter(
                      (v) => v !== SexualOrientationV2.PREFERNOTTOANSWER
                    )
                  : value
              );
            }}
            style={{ marginBottom: '1rem' }}
          />
          <MultipleChoiceQuestion
            question='4. What was the sex assigned to you at birth, such as on your original birth certificate?'
            options={[
              { label: 'Female', value: SexAtBirthV2.FEMALE },
              { label: 'Intersex', value: SexAtBirthV2.INTERSEX },
              { label: 'Male', value: SexAtBirthV2.MALE },
              {
                label: 'None of these fully describe me, and I want to specify',
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
              {
                label: 'Prefer not to answer',
                value: SexAtBirthV2.PREFERNOTTOANSWER,
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
          />
          <SemiBoldHeader style={{ marginBottom: '0.5rem' }}>
            Questions about disability status
          </SemiBoldHeader>
          <Divider style={{ marginTop: '.25rem' }} />
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
            difficulty doing errands alone such as visiting doctor's office or
              shopping?"
            selected={survey.disabilityErrands}
            onChange={(value) => onUpdate('disabilityErrands', value)}
            style={{ marginBottom: '1rem' }}
          />
          <FlexColumn>
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
          <SemiBoldHeader style={{ marginBottom: '0.5rem' }}>
            Other Questions
          </SemiBoldHeader>
          <Divider style={{ marginTop: '.25rem' }} />
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
            options={[
              { label: 'No Education', value: EducationV2.NOEDUCATION },
              { label: 'Grades 1-12', value: EducationV2.GRADES112 },
              { label: 'College Graduate', value: EducationV2.COLLEGEGRADUATE },
              { label: 'Undergraduate', value: EducationV2.UNDERGRADUATE },
              { label: "Master's", value: EducationV2.MASTER },
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
        </FlexColumn>
      </FlexColumn>
    );
  }
);

export default DemographicSurvey;
