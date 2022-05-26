import * as React from 'react';
import { CSSProperties, useState } from 'react';
import * as fp from 'lodash/fp';
import validate from 'validate.js';

import { Button } from 'app/components/buttons';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { Header, SmallHeader } from 'app/components/headers';
import { CheckBox, NumberInput, RadioButton } from 'app/components/inputs';
import { TooltipTrigger } from 'app/components/popups';
import { withProfileErrorModal } from 'app/components/with-error-modal';
import { reactStyles, useId } from 'app/utils';

const styles = reactStyles({
  question: { fontWeight: 'bold' },
  answer: { margin: '0.0rem 0.25rem' },
});

const maxYear = new Date().getFullYear();
const minYear = maxYear - 125;

const Option = (props: {
  checked: boolean;
  option: string;
  style?: CSSProperties;
  onChange: (any) => void;
  multiple?: boolean;
  disabled?: boolean;
  disabledText?: string;
}) => {
  const { checked, disabled, disabledText, multiple, onChange, option } = props;
  const id = useId();
  return (
    <TooltipTrigger
      content={disabled && disabledText && <div>{disabledText}</div>}
    >
      <FlexRow style={{ alignItems: 'center' }}>
        {multiple ? (
          <CheckBox
            data-test-id='nothing-to-report'
            manageOwnState={false}
            id={id}
            disabled={disabled}
            checked={checked}
            onChange={onChange}
          />
        ) : (
          <RadioButton
            data-test-id='nothing-to-report'
            id={id}
            disabled={disabled}
            checked={checked}
            onChange={onChange}
            value={option}
          />
        )}
        <label htmlFor={id} style={styles.answer}>
          {option}
        </label>
      </FlexRow>
    </TooltipTrigger>
  );
};

interface MultipleChoiceOption {
  name: string;
  otherText?: string;
  showInput?: boolean;
  onChange?: (any) => void;
  disabled?: boolean;
  disabledText?: string;
}

const MultipleChoiceQuestion = (props: {
  question: string;
  choices: MultipleChoiceOption[];
  selected: string | string[];
  onChange: (any) => void;
  style?: CSSProperties;
  multiple?: boolean;
}) => {
  const { choices, onChange, question, selected, multiple, style } = props;

  const handleChange = (e, name) => {
    if (multiple) {
      const result = e
        ? [...(selected as string[]), name]
        : (selected as string[]).filter((r) => r !== name);
      onChange(result);
    } else {
      onChange(e.target.value);
    }
  };

  return (
    <FlexRow style={{ ...style, alignItems: 'center' }}>
      <div style={{ ...styles.question, flex: 1, paddingRight: '1rem' }}>
        {question}
      </div>
      <FlexRow style={{ flex: 1, flexWrap: 'wrap', gap: '0.5rem' }}>
        {choices.map((choice) => (
          <FlexColumn>
            <Option
              disabled={choice.disabled}
              disabledText={choice.disabledText}
              option={choice.name}
              checked={
                multiple
                  ? selected.includes(choice.name)
                  : selected === choice.name
              }
              onChange={(e) => handleChange(e, choice.name)}
              multiple={multiple}
            />

            {choice.showInput &&
              ((multiple && selected.includes(choice.name)) ||
                selected === choice.name) && (
                <input
                  data-test-id='search'
                  style={{
                    marginBottom: '.5em',
                    marginLeft: '0.75rem',
                    width: '300px',
                  }}
                  type='text'
                  placeholder='Search'
                  value={choice.otherText}
                  onChange={choice.onChange}
                />
              )}
          </FlexColumn>
        ))}
      </FlexRow>
    </FlexRow>
  );
};

const YesNoOptionalQuestion = (props: {
  question: string;
  selected: string;
  onChange: (any) => void;
  style?: CSSProperties;
}) => {
  const { question, selected, onChange, style } = props;

  return (
    <MultipleChoiceQuestion
      question={question}
      choices={[
        { name: 'Yes' },
        { name: 'No' },
        { name: 'Prefer not to answer' },
      ]}
      selected={selected}
      onChange={onChange}
      style={style}
    />
  );
};

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
  const [education, setEducation] = useState(null),
    [genderIdentity, setGenderIdentity] = useState([]),
    [genderIdentityOtherText, setGenderIdentityOtherText] = useState(null),
    [sexAtBirth, setSexAtBirth] = useState(null),
    [sexAtBirthOtherText, setSexAtBirthOtherText] = useState(null),
    [sexualOrientation, setSexualOrientation] = useState([]),
    [sexualOrientationOtherText, setSexualOrientationOtherText] =
      useState(null),
    [deaf, setDeaf] = useState(null),
    [blind, setBlind] = useState(null),
    [concentration, setConcentration] = useState(null),
    [walking, setWalking] = useState(null),
    [dressing, setDressing] = useState(null),
    [errands, setErrands] = useState(null),
    [otherLifeActivity, setOtherLifeActivity] = useState(null),
    [raceEthnicity, setRaceEthnicity] = useState([]),
    [raceEthnicityOtherText, setRaceEthnicityOtherText] = useState(null),
    [aianOtherText, setAianOtherText] = useState(null),
    [asianOtherText, setAsianOtherText] = useState(null),
    [yearOfBirth, setYearOfBirth] = useState(null),
    handleYearOfBirthBlur = () => {
      if (yearOfBirth < minYear || yearOfBirth > maxYear) {
        setYearOfBirth(null);
      }
    };

  const demographicSurvey = {
    education,
    genderIdentityList: genderIdentity,
    race: raceEthnicity,
    sexAtBirth,
    yearOfBirth,
  };

  // Turn this into a state variable and make a useEffect for updating it.
  const errors = validateDemographicSurvey(demographicSurvey);

  const isAiAn =
    raceEthnicity.filter((s) => s.includes('American Indian or Alaska Native'))
      .length > 0;

  console.log('What is a demographicSurvey? ', demographicSurvey);
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
              otherText: aianOtherText,
              onChange: setAianOtherText,
            },
            {
              name: 'Asian',
            },
            { name: 'Indian' },
            { name: 'Cambodian' },
            { name: 'Chinese' },
            { name: 'Filipino' },
            { name: 'Hmong' },
            { name: 'Japanese' },
            { name: 'Korean' },
            { name: 'Lao' },
            { name: 'Pakistani' },
            { name: 'Vietnamese' },
            {
              name: 'Asian Other',
              showInput: true,
              otherText: asianOtherText,
              onChange: setAsianOtherText,
            },
            { name: 'Black, African American, or of African descent' },
            { name: 'Hispanic, Latino, or Spanish descent' },
            { name: 'Middle Eastern or North African' },
            { name: 'Native Hawaiian or other Pacific Islander' },
            { name: 'White, or of European descent' },
            {
              name: 'None of these fully describe me, and I want to specify',
              showInput: true,
              otherText: raceEthnicityOtherText,
              onChange: setRaceEthnicityOtherText,
            },
            { name: 'Prefer not to answer' },
          ]}
          multiple
          selected={raceEthnicity}
          onChange={setRaceEthnicity}
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
              disabled: !isAiAn,
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
              otherText: genderIdentityOtherText,
              onChange: setGenderIdentityOtherText,
            },
            { name: 'Prefer not to answer' },
          ]}
          multiple
          selected={genderIdentity}
          onChange={setGenderIdentity}
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
              disabled: !isAiAn,
              disabledText:
                'Two Spirit is an identity unique to people of American Indian and Alaska Native ' +
                'ancestry. If this applies to you, please update your selection in the ' +
                '"Race and Ethnicities" section.',
            },
            {
              name: 'None of these fully describe me, and I want to specify',
              showInput: true,
              otherText: sexualOrientationOtherText,
              onChange: setSexualOrientationOtherText,
            },
            { name: 'Prefer not to answer' },
          ]}
          selected={sexualOrientation}
          onChange={setSexualOrientation}
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
              otherText: sexAtBirthOtherText,
              onChange: setSexAtBirthOtherText,
            },
            { name: 'Prefer not to answer' },
          ]}
          selected={sexAtBirth}
          onChange={setSexAtBirth}
        />
        <SmallHeader style={{ marginBottom: '0.5rem' }}>
          Questions about disability status
        </SmallHeader>
        <YesNoOptionalQuestion
          question='Are you deaf or do you have serious difficulty hearing?'
          selected={deaf}
          onChange={setDeaf}
          style={{ marginBottom: '1rem' }}
        />
        <YesNoOptionalQuestion
          question='Are you blind or do you have serious difficulty seeing, even when
            wearing glasses?'
          selected={blind}
          onChange={setBlind}
          style={{ marginBottom: '1rem' }}
        />
        <YesNoOptionalQuestion
          question='Because of a physical, cognitive, or emotional condition, do you
            have serious difficulty concentrating, remembering, or making
            decisions?'
          selected={concentration}
          onChange={setConcentration}
          style={{ marginBottom: '1rem' }}
        />
        <YesNoOptionalQuestion
          question='Do you have serious difficulty walking or climbing stairs?'
          selected={walking}
          onChange={setWalking}
          style={{ marginBottom: '1rem' }}
        />
        <YesNoOptionalQuestion
          question='Do you have difficulty dressing or bathing?'
          selected={dressing}
          onChange={setDressing}
          style={{ marginBottom: '1rem' }}
        />
        <YesNoOptionalQuestion
          question="Because of a physical, mental, or emotional condition, do you have
            difficulty doing errands alone such as visiting doctor's office or
              shopping?"
          selected={errands}
          onChange={setErrands}
          style={{ marginBottom: '1rem' }}
        />
        <YesNoOptionalQuestion
          question='Do you have a physical, cognitive, and/or emotional condition that
            substantially inhibits one or more life activities not specified
            through the above questions, and want to share more? Please
            describe.'
          selected={otherLifeActivity}
          onChange={setOtherLifeActivity}
        />
        <SmallHeader style={{ marginBottom: '0.5rem' }}>
          Other Questions
        </SmallHeader>
        <div style={styles.question}>Year of birth</div>
        <NumberInput
          onChange={setYearOfBirth}
          onBlur={handleYearOfBirthBlur}
          min={minYear}
          max={maxYear}
          value={yearOfBirth}
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
          selected={education}
          onChange={setEducation}
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
