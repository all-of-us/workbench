import * as React from 'react';
import { useEffect, useState } from 'react';
import * as fp from 'lodash/fp';
import validate from 'validate.js';

import { Button } from 'app/components/buttons';
import { FlexColumn } from 'app/components/flex';
import { Header, SmallHeader } from 'app/components/headers';
import { NumberInput } from 'app/components/inputs';
import { TooltipTrigger } from 'app/components/popups';
import { withProfileErrorModal } from 'app/components/with-error-modal';

import { MultipleChoiceQuestion } from './components/multiple-choice-question';
import { styles } from './components/styles';
import { YesNoOptionalQuestion } from './components/yes-no-optional-question';

const maxYear = new Date().getFullYear();
const minYear = maxYear - 125;

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

const DemographicSurvey = fp.flow(withProfileErrorModal)(() => {
  const [survey, setSurvey] = useState({
    aianOtherText: '',
    asianOtherText: '',
    blind: null,
    concentration: null,
    deaf: null,
    dressing: null,
    education: null,
    errands: null,
    genderIdentity: [],
    genderIdentityOtherText: null,
    otherLifeActivity: null,
    raceEthnicity: [],
    raceEthnicityOtherText: null,
    sexAtBirth: null,
    sexAtBirthOtherText: null,
    sexualOrientation: [],
    sexualOrientationOtherText: null,
    walking: null,
    yearOfBirth: null,
  });
  const [isAian, setIsAian] = useState(false);

  useEffect(() => {
    setIsAian(
      survey.raceEthnicity.filter((s) =>
        s.includes('American Indian or Alaska Native')
      ).length > 0
    );
  }, [survey.raceEthnicity]);

  const handleInputChange = (prop, value) => {
    setSurvey({
      ...survey,
      [prop]: value,
    });
  };

  const handleYearOfBirthBlur = () => {
    if (survey.yearOfBirth < minYear || survey.yearOfBirth > maxYear) {
      handleInputChange('walking', null);
    }
  };

  // Turn this into a state variable and make a useEffect for updating it.
  const errors = validateDemographicSurvey(survey);

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
              otherText: survey.aianOtherText,
              onChange: (value) => handleInputChange('aianOtherText', value),
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
              otherText: survey.asianOtherText,
              onChange: (value) => handleInputChange('asianOtherText', value),
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
              otherText: survey.raceEthnicityOtherText,
              onChange: (value) =>
                handleInputChange('setRaceEthnicityOtherText', value),
            },
            { name: 'Prefer not to answer' },
          ]}
          multiple
          selected={survey.raceEthnicity}
          onChange={(value) => handleInputChange('raceEthnicity', value)}
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
              otherText: survey.genderIdentityOtherText,
              onChange: (value) =>
                handleInputChange('genderIdentityOtherText', value),
            },
            { name: 'Prefer not to answer' },
          ]}
          multiple
          selected={survey.genderIdentity}
          onChange={(value) => handleInputChange('genderIdentity', value)}
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
              otherText: survey.sexualOrientationOtherText,
              onChange: (value) =>
                handleInputChange('sexualOrientationOtherText', value),
            },
            { name: 'Prefer not to answer' },
          ]}
          selected={survey.sexualOrientation}
          onChange={(value) => handleInputChange('sexualOrientation', value)}
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
          selected={survey.deaf}
          onChange={(value) => handleInputChange('deaf', value)}
          style={{ marginBottom: '1rem' }}
        />
        <YesNoOptionalQuestion
          question='Are you blind or do you have serious difficulty seeing, even when
            wearing glasses?'
          selected={survey.blind}
          onChange={(value) => handleInputChange('blind', value)}
          style={{ marginBottom: '1rem' }}
        />
        <YesNoOptionalQuestion
          question='Because of a physical, cognitive, or emotional condition, do you
            have serious difficulty concentrating, remembering, or making
            decisions?'
          selected={survey.concentration}
          onChange={(value) => handleInputChange('concentration', value)}
          style={{ marginBottom: '1rem' }}
        />
        <YesNoOptionalQuestion
          question='Do you have serious difficulty walking or climbing stairs?'
          selected={survey.walking}
          onChange={(value) => handleInputChange('walking', value)}
          style={{ marginBottom: '1rem' }}
        />
        <YesNoOptionalQuestion
          question='Do you have difficulty dressing or bathing?'
          selected={survey.dressing}
          onChange={(value) => handleInputChange('dressing', value)}
          style={{ marginBottom: '1rem' }}
        />
        <YesNoOptionalQuestion
          question="Because of a physical, mental, or emotional condition, do you have
            difficulty doing errands alone such as visiting doctor's office or
              shopping?"
          selected={survey.errands}
          onChange={(value) => handleInputChange('errands', value)}
          style={{ marginBottom: '1rem' }}
        />
        <YesNoOptionalQuestion
          question='Do you have a physical, cognitive, and/or emotional condition that
            substantially inhibits one or more life activities not specified
            through the above questions, and want to share more? Please
            describe.'
          selected={survey.otherLifeActivity}
          onChange={(value) => handleInputChange('otherLifeActivity', value)}
        />
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
        />

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
                    You may select "Prefer not to answer" for each unfilled item
                    to continue
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
      </FlexColumn>
    </FlexColumn>
  );
});

export default DemographicSurvey;
