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
  radioAnswer: { marginRight: '0.25rem' },
});

const RadioOption = (props: {
  checked: boolean;
  option: string;
  style?: CSSProperties;
}) => {
  const { checked, option } = props;
  const id = useId();
  return (
    <FlexRow style={{ alignItems: 'center' }}>
      <RadioButton
        data-test-id='nothing-to-report'
        id={id}
        disabled={false}
        checked={checked}
        onChange={() => console.log('ff')}
      />
      <label htmlFor={id} style={styles.radioAnswer}>
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

const YesNoOptionalQuestion = (props: {
  question: string;
  style?: CSSProperties;
}) => {
  const { question, style } = props;

  return (
    <FlexRow {...{ style }}>
      <div style={{ ...styles.question, flex: 1, paddingRight: '1rem' }}>
        {question}
      </div>
      <FlexRow style={{ flex: 1 }}>
        <RadioOption option='Yes' checked={false} />
        <RadioOption option='No' checked={false} />
        <RadioOption option='Prefer not to answer' checked={false} />
      </FlexRow>
    </FlexRow>
  );
};

interface MultipleChoiceOption {
  name: string;
  value?: string;
  showInput?: boolean;
  onChange?: (any) => void;
}

const MultipleChoiceQuestion = (props: {
  question: string;
  choices: MultipleChoiceOption[];
  selected: string;
  style?: CSSProperties;
}) => {
  const { choices, question, selected, style } = props;

  return (
    <FlexRow {...{ style }}>
      <div style={{ ...styles.question, flex: 1, paddingRight: '1rem' }}>
        {question}
      </div>
      <FlexRow style={{ flex: 1, flexWrap: 'wrap', gap: '0.5rem' }}>
        {choices.map((choice) => (
          <FlexColumn>
            <RadioOption
              option={choice.name}
              checked={selected === choice.name}
            />
            {choice.showInput && selected === choice.name && (
              <input
                data-test-id='search'
                style={{ marginBottom: '.5em', width: '300px' }}
                type='text'
                placeholder='Search'
                onChange={(e) => console.log(e.target.value)}
              />
            )}
          </FlexColumn>
        ))}
      </FlexRow>
    </FlexRow>
  );
};

const DemographicSurvey = fp.flow(withProfileErrorModal)(
  (spinnerProps: WithSpinnerOverlayProps) => {
    const [checked, setChecked] = useState(false);
    return (
      <FlexColumn style={{ width: '750px', marginBottom: '10rem' }}>
        <Header>Researcher Workbench</Header>
        <FlexColumn>
          <SmallHeader>Races and Ethnicities</SmallHeader>
          <CategoryCheckbox
            {...{ checked }}
            category='American Indian or Alaska Native'
            onChange={(e) => setChecked(e)}
          />
          <CategoryCheckbox
            {...{ checked }}
            category='Asian'
            onChange={(e) => setChecked(e)}
          />
          <CategoryCheckbox
            {...{ checked }}
            category='Black, African American, or of African descent'
            onChange={(e) => setChecked(e)}
          />
          <CategoryCheckbox
            {...{ checked }}
            category='Hispanic, Latino, or Spanish descent'
            onChange={(e) => setChecked(e)}
          />
          <CategoryCheckbox
            {...{ checked }}
            category='Middle Eastern or North African'
            onChange={(e) => setChecked(e)}
          />
          <CategoryCheckbox
            {...{ checked }}
            category='Native Hawaiian or other Pacific Islander'
            onChange={(e) => setChecked(e)}
          />
          <CategoryCheckbox
            {...{ checked }}
            category='White, or of European descent'
            onChange={(e) => setChecked(e)}
          />
          <SmallHeader>Questions about genders</SmallHeader>
          <MultipleChoiceQuestion
            question='What terms best express how you describe your current gender identity?'
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
              },
              { name: 'Prefer not to answer' },
            ]}
            selected={'Doctorate'}
          />
          <MultipleChoiceQuestion
            question='What terms best express how you describe your current sexual orientation?'
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
              },
              { name: 'Prefer not to answer' },
            ]}
            selected={'Doctorate'}
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
              },
              { name: 'Prefer not to answer' },
            ]}
            selected={'Doctorate'}
          />
          <SmallHeader>Questions about disability status</SmallHeader>
          <YesNoOptionalQuestion
            question='Are you deaf or do you have serious difficulty hearing?'
            style={{ marginBottom: '1rem' }}
          />
          <YesNoOptionalQuestion
            question='Are you blind or do you have serious difficulty seeing, even when
            wearing glasses?'
            style={{ marginBottom: '1rem' }}
          />
          <YesNoOptionalQuestion
            question='Because of a physical, cognitive, or emotional condition, do you
            have serious difficulty concentrating, remembering, or making
            decisions?'
            style={{ marginBottom: '1rem' }}
          />
          <YesNoOptionalQuestion
            question='Do you have serious difficulty walking or climbing stairs?'
            style={{ marginBottom: '1rem' }}
          />
          <YesNoOptionalQuestion
            question='Do you have difficulty dressing or bathing?'
            style={{ marginBottom: '1rem' }}
          />
          <YesNoOptionalQuestion
            question="Because of a physical, mental, or emotional condition, do you have
            difficulty doing errands alone such as visiting doctor's office or
              shopping?"
            style={{ marginBottom: '1rem' }}
          />
          <YesNoOptionalQuestion
            question='Do you have a physical, cognitive, and/or emotional condition that
            substantially inhibits one or more life activities not specified
            through the above questions, and want to share more? Please
            describe.'
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
              { name: 'Doctorate', showInput: true },
              { name: 'Prefer not to answer' },
            ]}
            selected={'Doctorate'}
          />
        </FlexColumn>
      </FlexColumn>
    );
  }
);

export default DemographicSurvey;
