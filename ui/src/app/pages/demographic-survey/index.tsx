import * as React from 'react';
import { CSSProperties } from 'react';
import * as fp from 'lodash/fp';

import { FlexColumn, FlexRow } from 'app/components/flex';
import { Header, SmallHeader } from 'app/components/headers';
import { NumberInput, RadioButton } from 'app/components/inputs';
import { withProfileErrorModal } from 'app/components/with-error-modal';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import { reactStyles, useId } from 'app/utils';

const styles = reactStyles({
  question: { fontWeight: 'bold' },
  radioAnswer: { marginRight: '0.25rem' },
});

const RadioOption = (props: { option: string; style?: CSSProperties }) => {
  const { option } = props;
  const id = useId();
  return (
    <FlexRow style={{ alignItems: 'center' }}>
      <label htmlFor={id} style={styles.radioAnswer}>
        {option}
      </label>
      <RadioButton
        data-test-id='nothing-to-report'
        id={id}
        disabled={false}
        style={{ marginRight: '1rem' }}
        checked={false}
        onChange={() => console.log('ff')}
      />
    </FlexRow>
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
        <RadioOption option='Yes' />
        <RadioOption option='No' />
        <RadioOption option='Prefer not to answer' />
      </FlexRow>
    </FlexRow>
  );
};

const DemographicSurvey = fp.flow(withProfileErrorModal)(
  (spinnerProps: WithSpinnerOverlayProps) => {
    return (
      <FlexColumn style={{ width: '750px' }}>
        <Header>Researcher Workbench</Header>
        <FlexColumn>
          <SmallHeader>Races and Ethnicities</SmallHeader>
          <div>Select</div>
          <SmallHeader>Questions about genders</SmallHeader>
          <div style={styles.question}>
            What terms best express how you describe your current gender
            identity?
          </div>
          <div>Select</div>
          <div style={styles.question}>
            What terms best express how you describe your sexual orientation?
          </div>
          <div>Select</div>
          <div style={styles.question}>
            What terms best express how you describe your current gender
            identity?
          </div>
          <div>Select</div>
          <div style={styles.question}>
            What was the sex assigned to you at birth, such as on your original
            birth certificate?
          </div>
          <div>Select</div>
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
          <div>Highest Level of Education</div>
          <div>Select</div>
        </FlexColumn>
      </FlexColumn>
    );
  }
);

export default DemographicSurvey;
