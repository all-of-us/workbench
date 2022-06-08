import * as React from 'react';
import { useEffect, useState } from 'react';
import * as fp from 'lodash/fp';
import validate from 'validate.js';

import {
  EducationV2,
  EthnicCategory,
  GenderIdentityV2,
  SexAtBirthV2,
  SexualOrientationV2,
} from 'generated/fetch';

import { Button } from 'app/components/buttons';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { Header, SmallHeader } from 'app/components/headers';
import { TooltipTrigger } from 'app/components/popups';
import { withProfileErrorModal } from 'app/components/with-error-modal';
import { profileApi } from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import { reactStyles } from 'app/utils';
import { useNavigation } from 'app/utils/navigation';
import { profileStore } from 'app/utils/stores';

import { CheckBox, NumberInput, TextInput } from './inputs';
import { MultipleChoiceQuestion } from './multiple-choice-question';
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
  const validationCheck = {
    ethnicCategories: { presence: { allowEmpty: false } },
    genderIdentities: { presence: { allowEmpty: false } },
    sexAtBirth: { presence: { allowEmpty: false } },
    yearOfBirth: { presence: { allowEmpty: false } },
    education: { presence: { allowEmpty: false } },
  };
  return validate(demographicSurvey, validationCheck);
};

const DemographicSurvey = fp.flow(withProfileErrorModal)((props) => {
  const [, navigateByUrl] = useNavigation();
  const [captchaToken, setCaptchaToken] = useState('');
  const [loading, setLoading] = useState(false);
  const [errors, setErrors] = useState(null);
  const [survey, setSurvey] = useState({
    education: null,
    ethnicityAiAnOtherText: null,
    ethnicityAsianOtherText: null,
    ethnicCategories: [],
    ethnicityOtherText: null,
    disabilityConcentrating: null,
    disabilityDressing: null,
    disabilityErrands: null,
    disabilityHearing: null,
    disabilityOtherText: null,
    disabilitySeeing: null,
    disabilityWalking: null,
    disadvantaged: null,
    genderIdentities: [],
    genderOtherText: null,
    orientationOtherText: null,
    sexAtBirth: null,
    sexAtBirthOtherText: null,
    sexualOrientations: [],
    yearOfBirth: null,
    yearOfBirthPreferNot: false,
  });
  const [isAian, setIsAian] = useState(false);
  const [showAsianOptions, setShowAsianOptions] = useState(false);
  const [showAiAnOptions, setShowAiAnOptions] = useState(false);

  // const { profile, saveProfile } = props;

  useEffect(() => {
    setErrors(validateDemographicSurvey(survey));
  }, [survey]);

  useEffect(() => {
    setIsAian(
      survey.ethnicCategories.filter(
        (s) =>
          s.includes(EthnicCategory.AIAN) ||
          s.includes(EthnicCategory.AIANCENTRALSOUTH) ||
          s.includes(EthnicCategory.AIANOTHER)
      ).length > 0
    );
  }, [survey.ethnicCategories]);

  const handleInputChange = (prop, value) => {
    setSurvey({
      ...survey,
      [prop]: value,
    });
  };

  const handleYearOfBirthBlur = () => {
    if (survey.yearOfBirth < minYear || survey.yearOfBirth > maxYear) {
      handleInputChange('yearOfBirth', null);
    }
  };

  const handleYearOfBirthPreferNotChange = (value) => {
    setSurvey({
      ...survey,
      yearOfBirth: value && null,
      yearOfBirthPreferNot: value,
    });
  };

  const saveSurvey = async () => {
    setLoading(true);
    const profile = profileStore.get().profile;

    const newProfile = { ...profile, demographicSurveyV2: survey };
    await profileApi().updateProfile(newProfile, {});

    setLoading(false);
  };

  return (
    <FlexColumn style={{ width: '750px', marginBottom: '10rem' }}>
      <Header>Researcher Workbench</Header>
      <div style={{ color: colors.primary }}>
        The All of Us Research Program is dedicated to cultivating a diverse
        research community and building an inclusive platform. Your answers to
        these questions will help us learn more about who is using the platform.
        Your privacy is important to us. Your individual responses will not be
        displayed on your researcher profile and will only be accessible to a
        limited number of authorized All of Us personnel. All of Us may
        externally report de-identified, aggregate data on the demographics of
        individuals using the Researcher Workbench.
      </div>
      <FlexColumn>
        <SmallHeader>Races and Ethnicities</SmallHeader>
        <MultipleChoiceQuestion
          question='Which races and/or ethnicities do you identify with? Please select all that apply.'
          options={[
            {
              label: 'American Indian or Alaska Native',
              value: EthnicCategory.AIAN,
              subOptions: [
                {
                  label:
                    'American Indian or Alaska Native / Central or South American Indian',
                  value: EthnicCategory.AIANCENTRALSOUTH,
                },
                {
                  label:
                    'American Indian or Alaska Native / None of these fully describe me, and I want to specify',
                  value: EthnicCategory.AIANOTHER,
                  showInput: true,
                  otherText: survey.ethnicityAiAnOtherText,
                  onChangeOtherText: (value) => {
                    handleInputChange('ethnicityAiAnOtherText', value);
                  },
                },
              ],
              showSubOptions: showAiAnOptions,
              onChange: (value) => setShowAiAnOptions(value),
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
                  label: 'Asian Other',
                  value: EthnicCategory.ASIANOTHER,
                  showInput: true,
                  otherText: survey.ethnicityAsianOtherText,
                  onChangeOtherText: (value) =>
                    handleInputChange('ethnicityAsianOtherText', value),
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
            },
            {
              label: 'Hispanic, Latino, or Spanish descent',
              value: EthnicCategory.HISPANIC,
            },
            {
              label: 'Middle Eastern or North African',
              value: EthnicCategory.MENA,
            },
            {
              label: 'Native Hawaiian or other Pacific Islander',
              value: EthnicCategory.NHPI,
            },
            {
              label: 'White, or of European descent',
              value: EthnicCategory.WHITE,
            },
            {
              label: 'None of these fully describe me, and I want to specify',
              value: EthnicCategory.OTHER,
              showInput: true,
              otherText: survey.ethnicityOtherText,
              onChangeOtherText: (value) =>
                handleInputChange('ethnicityOtherText', value),
            },
            {
              label: 'Prefer not to answer',
              value: EthnicCategory.PREFERNOTTOANSWER,
            },
          ]}
          multiple
          selected={survey.ethnicCategories}
          onChange={(value) => handleInputChange('ethnicCategories', value)}
          style={{ marginBottom: '1rem' }}
        />
        <SmallHeader>Questions about gender</SmallHeader>
        <MultipleChoiceQuestion
          question='What terms best express how you describe your current gender identity?'
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
              onChangeOtherText: (value) =>
                handleInputChange('genderOtherText', value),
            },
            {
              label: 'Prefer not to answer',
              value: GenderIdentityV2.PREFERNOTTOANSWER,
            },
          ]}
          multiple
          selected={survey.genderIdentities}
          onChange={(value) => handleInputChange('genderIdentities', value)}
          style={{ marginBottom: '1rem' }}
        />
        <MultipleChoiceQuestion
          question='What terms best express how you describe your current sexual orientation?'
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
              onChangeOtherText: (value) =>
                handleInputChange('orientationOtherText', value),
            },
            {
              label: 'Prefer not to answer',
              value: SexualOrientationV2.PREFERNOTTOANSWER,
            },
          ]}
          multiple
          selected={survey.sexualOrientations}
          onChange={(value) => handleInputChange('sexualOrientations', value)}
          style={{ marginBottom: '1rem' }}
        />
        <MultipleChoiceQuestion
          question='What was the sex assigned to you at birth, such as on your original birth certificate?'
          options={[
            { label: 'Female', value: SexAtBirthV2.FEMALE },
            { label: 'Intersex', value: SexAtBirthV2.INTERSEX },
            { label: 'Male', value: SexAtBirthV2.MALE },
            {
              label: 'None of these fully describe me, and I want to specify',
              value: SexAtBirthV2.OTHER,
              showInput: true,
              otherText: survey.sexAtBirthOtherText,
              onChangeOtherText: (value) =>
                handleInputChange('sexAtBirthOtherText', value),
            },
            {
              label: 'Prefer not to answer',
              value: SexAtBirthV2.PREFERNOTTOANSWER,
            },
          ]}
          selected={survey.sexAtBirth}
          onChange={(value) => handleInputChange('sexAtBirth', value)}
        />
        <SmallHeader style={{ marginBottom: '0.5rem' }}>
          Questions about disability status
        </SmallHeader>
        <YesNoOptionalQuestion
          question='Are you deaf or do you have serious difficulty hearing?'
          selected={survey.disabilityHearing}
          onChange={(value) => handleInputChange('disabilityHearing', value)}
          style={{ marginBottom: '1rem' }}
        />
        <YesNoOptionalQuestion
          question='Are you blind or do you have serious difficulty seeing, even when
            wearing glasses?'
          selected={survey.disabilitySeeing}
          onChange={(value) => handleInputChange('disabilitySeeing', value)}
          style={{ marginBottom: '1rem' }}
        />
        <YesNoOptionalQuestion
          question='Because of a physical, cognitive, or emotional condition, do you
            have serious difficulty concentrating, remembering, or making
            decisions?'
          selected={survey.disabilityConcentrating}
          onChange={(value) =>
            handleInputChange('disabilityConcentrating', value)
          }
          style={{ marginBottom: '1rem' }}
        />
        <YesNoOptionalQuestion
          question='Do you have serious difficulty walking or climbing stairs?'
          selected={survey.disabilityWalking}
          onChange={(value) => handleInputChange('disabilityWalking', value)}
          style={{ marginBottom: '1rem' }}
        />
        <YesNoOptionalQuestion
          question='Do you have difficulty dressing or bathing?'
          selected={survey.disabilityDressing}
          onChange={(value) => handleInputChange('disabilityDressing', value)}
          style={{ marginBottom: '1rem' }}
        />
        <YesNoOptionalQuestion
          question="Because of a physical, mental, or emotional condition, do you have
            difficulty doing errands alone such as visiting doctor's office or
              shopping?"
          selected={survey.disabilityErrands}
          onChange={(value) => handleInputChange('disabilityErrands', value)}
          style={{ marginBottom: '1rem' }}
        />
        <FlexRow style={{ alignItems: 'center', gap: '1.75rem' }}>
          <div style={{ ...styles.question, flex: 1 }}>
            Do you have a physical, cognitive, and/or emotional condition that
            substantially inhibits one or more life activities not specified
            through the above questions, and want to share more? Please
            describe.
          </div>
          <TextInput
            onChange={(value) =>
              handleInputChange('disabilityOtherText', value)
            }
            value={survey.disabilityOtherText || ''}
            style={{ flex: 1 }}
          />
        </FlexRow>
        <SmallHeader style={{ marginBottom: '0.5rem' }}>
          Other Questions
        </SmallHeader>

        <div style={styles.question}>Year of birth</div>

        <FlexRow style={{ alignItems: 'center' }}>
          <NumberInput
            onChange={(value) => handleInputChange('yearOfBirth', value)}
            onBlur={handleYearOfBirthBlur}
            disabled={survey.yearOfBirthPreferNot}
            min={minYear}
            max={maxYear}
            value={survey.yearOfBirth || ''}
            style={{ width: '4rem' }}
          />
          <CheckBox
            id='show-again-checkbox'
            checked={survey.yearOfBirthPreferNot}
            onChange={(value) => handleYearOfBirthPreferNotChange(value)}
            style={{ marginLeft: '1rem' }}
          />
          <label
            htmlFor='show-again-checkbox'
            style={{
              marginLeft: '8px',
              marginRight: 'auto',
            }}
          >
            Prefer not to answer
          </label>
        </FlexRow>
        <MultipleChoiceQuestion
          question={'Highest Level of Education'}
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
          onChange={(value) => handleInputChange('education', value)}
          style={{ marginBottom: '1rem' }}
        />

        <YesNoOptionalQuestion
          question='Are you an individual from a disadvantaged background, as defined by NIH Diversity in Extramural Programs?'
          selected={survey.disadvantaged}
          onChange={(value) => handleInputChange('disadvantaged', value)}
        />

        <FlexRow>
          {props.onPreviousClick && (
            <Button
              disabled={errors}
              type='primary'
              onClick={(_) => console.log('Save')}
              data-test-id={'submit-button'}
              style={{ marginTop: '3rem', marginRight: '1rem' }}
            >
              Previous
            </Button>
          )}
          <TooltipTrigger
            content={
              errors && (
                <>
                  <div>Please review the following:</div>
                  <ul>
                    {Object.keys(errors).map((key) => (
                      <li key={errors[key][0]}>{errors[key][0]}</li>
                    ))}
                    <li>
                      You may select "Prefer not to answer" for each unfilled
                      item to continue
                    </li>
                  </ul>
                </>
              )
            }
          >
            <Button
              disabled={false}
              type='primary'
              onClick={saveSurvey}
              data-test-id={'submit-button'}
              style={{ marginTop: '3rem' }}
            >
              Submit
            </Button>
          </TooltipTrigger>
        </FlexRow>
      </FlexColumn>
    </FlexColumn>
  );
});

export default DemographicSurvey;
