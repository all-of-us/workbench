import * as React from 'react';
import { useEffect, useState } from 'react';
import * as fp from 'lodash/fp';
import validate from 'validate.js';

import { Button } from 'app/components/buttons';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { Header, SmallHeader } from 'app/components/headers';
import { NumberInput, TextInput } from 'app/components/inputs';
import { TooltipTrigger } from 'app/components/popups';
import { withProfileErrorModal } from 'app/components/with-error-modal';
import { reactStyles } from 'app/utils';

import { MultipleChoiceQuestion } from './multiple-choice-question';
import { YesNoOptionalQuestion } from './yes-no-optional-question';

const maxYear = new Date().getFullYear();
const minYear = maxYear - 125;
const styles = reactStyles({
  question: { fontWeight: 'bold' },
  answer: { margin: '0.0rem 0.25rem' },
});

const validateDemographicSurvey = (demographicSurvey) => {
  validate.validators.nullBoolean = (v) =>
    v === true || v === false || v === null
      ? undefined
      : 'value must be selected';
  const validationCheck = {
    race: { presence: { allowEmpty: false } },
    genderIdentityList: { presence: { allowEmpty: false } },
    sexAtBirth: { presence: { allowEmpty: false } },
    yearOfBirth: { presence: { allowEmpty: false } },
    education: { presence: { allowEmpty: false } },
  };
  return validate(demographicSurvey, validationCheck);
};

const DemographicSurvey = fp.flow(withProfileErrorModal)((props) => {
  const [captchaToken, setCaptchaToken] = useState('');
  const [loading, setLoading] = useState(false);
  const [errors, setErrors] = useState(null);
  const [survey, setSurvey] = useState({
    education: null,
    ethnicityAiAnOtherText: '',
    ethnicityAsianOtherText: '',
    ethnicCategories: [],
    ethnicityOtherText: null,
    disabilityConcentrating: null,
    disabilityDressing: null,
    disabilityErrands: null,
    disabilityHearing: null,
    // ???? Should this have a yes no (yes if text, no otherwise, what about prefer not?)
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
    yearOfBirthPreferNot: null,
  });
  const [isAian, setIsAian] = useState(false);

  const { profile, saveProfile } = props;

  useEffect(() => {
    setErrors(validateDemographicSurvey(survey));
  }, [survey]);

  useEffect(() => {
    setIsAian(
      survey.ethnicCategories.filter((s) =>
        s.includes('American Indian or Alaska Native')
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

  // const saveSurvey = async () => {
  //   setLoading(true);
  //
  //   try {
  //     const newProfile = { ...profile, demographicSurveyV2: survey };
  //     const savedProfile = await props.saveProfile(newProfile, captchaToken);
  //
  //     Still need to update this
  //     this.setState((prevState) => ({
  //       profile: savedProfile || prevState.profile,
  //       loading: false,
  //     }));
  //   } catch (error) {
  //     reportError(error);
  //     const { message } = await convertAPIError(error);
  //     this.props.showProfileErrorModal(message);
  //     if (environment.enableCaptcha && this.props.enableCaptcha) {
  //       // Reset captcha
  //       this.captchaRef.current.reset();
  //       this.setState({ captcha: false });
  //     }
  //     this.setState({ loading: false });
  //   }
  // };

  return (
    <FlexColumn style={{ width: '750px', marginBottom: '10rem' }}>
      <Header>Researcher Workbench</Header>
      <FlexColumn>
        <SmallHeader>Races and Ethnicities</SmallHeader>
        <MultipleChoiceQuestion
          question='Which races and/or ethnicities do you identify with? Please select all that apply.'
          choices={[
            { name: 'American Indian or Alaska Native' },
            {
              name: 'American Indian or Alaska Native / Central or South American Indian',
            },
            {
              name: 'American Indian or Alaska Native / None of these fully describe me, and I want to specify',
              showInput: true,
              otherText: survey.ethnicityAiAnOtherText,
              onChange: (value) =>
                handleInputChange('ethnicityAiAnOtherText', value),
            },
            ...[
              'Asian',
              'Indian',
              'Cambodian',
              'Chinese',
              'Filipino',
              'Hmong',
              'Japanese',
              'Korean',
              'Lao',
              'Pakistani',
              'Vietnamese',
            ].map((name) => {
              return {
                name,
              };
            }),
            {
              name: 'Asian Other',
              showInput: true,
              otherText: survey.ethnicityAsianOtherText,
              onChange: (value) =>
                handleInputChange('ethnicityAsianOtherText', value),
            },
            ...[
              'Black, African American, or of African descent',
              'Hispanic, Latino, or Spanish descent',
              'Middle Eastern or North African',
              'Native Hawaiian or other Pacific Islander',
              'White, or of European descent',
            ].map((name) => {
              return {
                name,
              };
            }),
            {
              name: 'None of these fully describe me, and I want to specify',
              showInput: true,
              otherText: survey.ethnicityOtherText,
              onChange: (value) =>
                handleInputChange('setEthnicityOtherText', value),
            },
            { name: 'Prefer not to answer' },
          ]}
          multiple
          selected={survey.ethnicCategories}
          onChange={(value) => handleInputChange('ethnicCategories', value)}
          style={{ marginBottom: '3rem' }}
        />
        <SmallHeader>Questions about gender</SmallHeader>
        <MultipleChoiceQuestion
          question='What terms best express how you describe your current gender identity?'
          choices={[
            ...[
              'Gender Queer',
              'Man',
              'Non-binary',
              'Questioning or unsure of my gender identity',
              'Trans man/Transgender man',
              'Trans woman/Transgender woman',
            ].map((name) => {
              return {
                name,
              };
            }),
            {
              name: 'Two Spirit',
              disabled: !isAian,
              disabledText:
                'Two Spirit is an identity unique to people of American Indian and Alaska Native ' +
                'ancestry. If this applies to you, please update your selection in the ' +
                '"Race and Ethnicities" section.',
            },
            {
              name: 'Woman',
            },
            {
              name: 'None of these fully describe me, and I want to specify',
              showInput: true,
              otherText: survey.genderOtherText,
              onChange: (value) => handleInputChange('genderOtherText', value),
            },
            { name: 'Prefer not to answer' },
          ]}
          multiple
          selected={survey.genderIdentities}
          onChange={(value) => handleInputChange('genderIdentities', value)}
          style={{ marginBottom: '3rem' }}
        />
        <MultipleChoiceQuestion
          question='What terms best express how you describe your current sexual orientation?'
          choices={[
            ...[
              'Asexual',
              'Bisexual',
              'Gay',
              'Lesbian',
              'Polysexual, omnisexual, or pansexual',
              'Queer',
              'Questioning or unsure of my sexual orientation',
              'Same-gender loving',
              'Straight or heterosexual',
            ].map((name) => {
              return {
                name,
              };
            }),
            {
              name: 'Two Spirit',
              disabled: !isAian,
              disabledText:
                'Two Spirit is an identity unique to people of American Indian and Alaska Native ' +
                'ancestry. If this applies to you, please update your selection in the ' +
                '"Race and Ethnicities" section.',
            },
            {
              name: 'None of these fully describe me, and I want to specify',
              showInput: true,
              otherText: survey.orientationOtherText,
              onChange: (value) =>
                handleInputChange('orientationOtherText', value),
            },
            { name: 'Prefer not to answer' },
          ]}
          selected={survey.sexualOrientations}
          onChange={(value) => handleInputChange('sexualOrientations', value)}
          style={{ marginBottom: '3rem' }}
        />
        <MultipleChoiceQuestion
          question='What was the sex assigned to you at birth, such as on your original birth certificate?'
          choices={[
            ...['Female', 'Intersex', 'Male'].map((name) => {
              return {
                name,
              };
            }),
            {
              name: 'None of these fully describe me, and I want to specify',
              showInput: true,
              otherText: survey.sexAtBirthOtherText,
              onChange: (value) =>
                handleInputChange('sexAtBirthOtherText', value),
            },
            { name: 'Prefer not to answer' },
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
        <YesNoOptionalQuestion
          question='Do you have a physical, cognitive, and/or emotional condition that
            substantially inhibits one or more life activities not specified
            through the above questions, and want to share more? Please
            describe.'
          selected={survey.disabilityOtherText}
          onChange={(value) => handleInputChange('disabilityOtherText', value)}
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
            value={survey.disabilityOtherText}
            style={{ flex: 1 }}
          />
        </FlexRow>
        <SmallHeader style={{ marginBottom: '0.5rem' }}>
          Other Questions
        </SmallHeader>
        <div style={styles.question}>Year of birth</div>
        <NumberInput
          onChange={(value) => handleInputChange('yearOfBirth', value)}
          onBlur={handleYearOfBirthBlur}
          min={minYear}
          max={maxYear}
          value={survey.yearOfBirth}
          style={{ width: '4rem' }}
        />
        <MultipleChoiceQuestion
          question={'Highest Level of Education'}
          choices={[
            ...[
              'No Education',
              'Grades 1-12',
              'College Graduate',
              'Undergraduate',
              "Master's",
              'Doctorate',
              'Prefer not to answer',
            ].map((name) => {
              return {
                name,
              };
            }),
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
              disabled={errors}
              type='primary'
              onClick={(_) => console.log('Save')}
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
