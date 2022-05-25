import * as React from 'react';
import { CSSProperties, useState } from 'react';
import * as fp from 'lodash/fp';

import { FlexColumn, FlexRow } from 'app/components/flex';
import { Header, SmallHeader } from 'app/components/headers';
import { CheckBox, NumberInput, RadioButton } from 'app/components/inputs';
import { withProfileErrorModal } from 'app/components/with-error-modal';
import { reactStyles, useId } from 'app/utils';

const styles = reactStyles({
  question: { fontWeight: 'bold' },
  answer: { margin: '0.0rem 0.25rem' },
});

const minYear = '0';
const maxYear = '3000';

const Option = (props: {
  checked: boolean;
  option: string;
  style?: CSSProperties;
  onChange: (any) => void;
  multiple?: boolean;
}) => {
  const { checked, multiple, onChange, option } = props;
  const id = useId();
  return (
    <FlexRow style={{ alignItems: 'center' }}>
      {multiple ? (
        <CheckBox
          data-test-id='nothing-to-report'
          manageOwnState={false}
          id={id}
          disabled={false}
          checked={checked}
          onChange={onChange}
        />
      ) : (
        <RadioButton
          data-test-id='nothing-to-report'
          id={id}
          disabled={false}
          checked={checked}
          onChange={onChange}
          value={option}
        />
      )}
      <label htmlFor={id} style={styles.answer}>
        {option}
      </label>
    </FlexRow>
  );
};

interface MultipleChoiceOption {
  name: string;
  otherText?: string;
  showInput?: boolean;
  onChange?: (any) => void;
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

const DemographicSurvey = fp.flow(withProfileErrorModal)(() => {
  const [education, setEducation] = useState(null),
    [genderIdentity, setGenderIdentity] = useState(null),
    [genderIdentityOtherText, setGenderIdentityOtherText] = useState(null),
    [sexAssignedAtBirth, setSexAssignedAtBirth] = useState(null),
    [sexAssignedAtBirthOtherText, setSexAssignedAtBirthOtherText] =
      useState(null),
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
    [birthYear, setBirthYear] = useState(null),
    handleBirthYearBlur = () => {
      if (birthYear < minYear || birthYear > maxYear) {
        setBirthYear(null);
      }
    };
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
        <SmallHeader>Questions about genders</SmallHeader>
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
              'Two Spirit',
              'Woman',
            ].map((name) => {
              return {
                name,
              };
            }),
            {
              name: 'None of these fully describe me, and I want to specify',
              showInput: true,
              otherText: sexualOrientationOtherText,
              onChange: setSexualOrientationOtherText,
            },
            { name: 'Prefer not to answer' },
          ]}
          multiple
          selected={sexualOrientation}
          onChange={setSexualOrientation}
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
              'Two Spirit',
            ].map((name) => {
              return {
                name,
              };
            }),
            {
              name: 'None of these fully describe me, and I want to specify',
              showInput: true,
              otherText: genderIdentityOtherText,
              onChange: setGenderIdentityOtherText,
            },
            { name: 'Prefer not to answer' },
          ]}
          selected={genderIdentity}
          onChange={setGenderIdentity}
          style={{ marginBottom: '3rem' }}
        />
        <MultipleChoiceQuestion
          question='What was the sex assigned to you at birth, such as on your original birth certificate?'
          choices={[
            ...[
              'Gender Queer',
              'Man',
              'Non-binary',
              'Questioning or unsure of my gender identity',
              'Trans man/Transgender man',
              'Trans woman/Transgender woman',
              'Two Spirit',
              'Woman',
            ].map((name) => {
              return {
                name,
              };
            }),
            {
              name: 'None of these fully describe me, and I want to specify',
              showInput: true,
              otherText: sexAssignedAtBirthOtherText,
              onChange: setSexAssignedAtBirthOtherText,
            },
            { name: 'Prefer not to answer' },
          ]}
          selected={sexAssignedAtBirth}
          onChange={setSexAssignedAtBirth}
        />
        <SmallHeader>Questions about disability status</SmallHeader>
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
        <SmallHeader>Other Questions</SmallHeader>
        <div style={styles.question}>Year of birth</div>
        <NumberInput
          onChange={setBirthYear}
          onBlur={handleBirthYearBlur}
          min={minYear}
          max={maxYear}
          value={birthYear}
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
      </FlexColumn>
    </FlexColumn>
  );
});

export default DemographicSurvey;
