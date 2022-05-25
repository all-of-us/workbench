import * as React from 'react';
import { CSSProperties, useState } from 'react';
import * as fp from 'lodash/fp';

import { FlexColumn, FlexRow } from 'app/components/flex';
import { Header, SmallHeader } from 'app/components/headers';
import {
  CheckBox,
  NumberInput,
  RadioButton,
  Select,
} from 'app/components/inputs';
import { withProfileErrorModal } from 'app/components/with-error-modal';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import { reactStyles, useId } from 'app/utils';

const styles = reactStyles({
  question: { fontWeight: 'bold' },
  answer: { marginRight: '0.25rem' },
});

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

const CategoryCheckbox = (props: {
  checked: boolean;
  category: string;
  onChange: (any) => void;
}) => {
  const [value, setValue] = useState(null);
  return (
    <FlexColumn>
      <CheckBox
        label={props.category}
        data-test-id={`checkbox`}
        key={'value'}
        checked={props.checked}
        manageOwnState={false}
        onChange={props.onChange}
      />
      {props.checked && (
        <Select
          {...{ value }}
          styles={{
            menuPortal: (base) => ({
              ...base,
              zIndex: 110,
            }),
          }}
          menuPortalTarget={document.getElementById('popup-root')}
          isDisabled={false}
          classNamePrefix={''}
          data-test-id={''}
          onChange={(e) => setValue(e.value)}
          options={['A', 'B', 'C']}
        />
      )}
    </FlexColumn>
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
                  style={{ marginBottom: '.5em', width: '300px' }}
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

const DemographicSurvey = fp.flow(withProfileErrorModal)(
  (spinnerProps: WithSpinnerOverlayProps) => {
    const [checked, setChecked] = useState(false);
    const [education, setEducation] = useState(null);
    const [genderIdentity, setGenderIdentity] = useState(null);
    const [genderIdentityOtherText, setGenderIdentityOtherText] =
      useState(null);
    const [sexAssignedAtBirth, setSexAssignedAtBirth] = useState(null);
    const [sexAssignedAtBirthOtherText, setSexAssignedAtBirthOtherText] =
      useState(null);
    const [sexualOrientation, setSexualOrientation] = useState([]);
    const [sexualOrientationOtherText, setSexualOrientationOtherText] =
      useState(null);
    const [deaf, setDeaf] = useState(null);
    const [blind, setBlind] = useState(null);
    const [concentration, setConcentration] = useState(null);
    const [walking, setWalking] = useState(null);
    const [dressing, setDressing] = useState(null);
    const [errands, setErrands] = useState(null);
    const [otherLifeActivity, setOtherLifeActivity] = useState(null);
    const [raceEthnicity, setRaceEthnicity] = useState([]);
    const [raceEthnicityOtherText, setRaceEthnicityOtherText] = useState('');
    return (
      <FlexColumn style={{ width: '750px', marginBottom: '10rem' }}>
        <Header>Researcher Workbench</Header>
        <FlexColumn>
          <SmallHeader>Races and Ethnicities</SmallHeader>
          <MultipleChoiceQuestion
            question='Which races and/or ethnicities do you identify with? Please select all that apply.'
            choices={[
              { name: 'American Indian or Alaska Native' },
              { name: 'Asian' },
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
              { name: 'Gender Queer' },
              { name: 'Man' },
              { name: 'Non-binary' },
              { name: 'Questioning or unsure of my gender identity' },
              { name: 'Trans man/Transgender man' },
              { name: 'Trans woman/Transgender woman' },
              { name: 'Two Spirit' },
              { name: 'Woman' },
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
              { name: 'Asexual' },
              { name: 'Bisexual' },
              { name: 'Gay' },
              { name: 'Lesbian' },
              { name: 'Polysexual, omnisexual, or pansexual' },
              { name: 'Queer' },
              { name: 'Questioning or unsure of my sexual orientation' },
              { name: 'Same-gender loving' },
              { name: 'Straight or heterosexual' },
              { name: 'Two Spirit' },
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
              { name: 'Gender Queer' },
              { name: 'Man' },
              { name: 'Non-binary' },
              { name: 'Questioning or unsure of my gender identity' },
              { name: 'Trans man/Transgender man' },
              { name: 'Trans woman/Transgender woman' },
              { name: 'Two Spirit' },
              { name: 'Woman' },
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
          <div>Year of birth</div>
          <NumberInput
            onChange={() => console.log('A')}
            onBlur={() => console.log('A')}
          />
          <MultipleChoiceQuestion
            question={'Highest Level of Education'}
            choices={[
              { name: 'No Education' },
              { name: 'Grades 1-12' },
              { name: 'College Graduate' },
              { name: 'Undergraduate' },
              { name: "Master's" },
              { name: 'Doctorate' },
              { name: 'Prefer not to answer' },
            ]}
            selected={education}
            onChange={setEducation}
          />
        </FlexColumn>
      </FlexColumn>
    );
  }
);

export default DemographicSurvey;
