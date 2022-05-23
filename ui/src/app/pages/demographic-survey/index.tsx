import * as React from 'react';
import * as fp from 'lodash/fp';

import { FlexColumn } from 'app/components/flex';
import { Header, SmallHeader } from 'app/components/headers';
import { NumberInput } from 'app/components/inputs';
import { withProfileErrorModal } from 'app/components/with-error-modal';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import { reactStyles } from 'app/utils';

const styles = reactStyles({
  question: { fontWeight: 'bold' },
});

const DemographicSurvey = fp.flow(withProfileErrorModal)(
  (spinnerProps: WithSpinnerOverlayProps) => {
    return (
      <FlexColumn>
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
          <div style={styles.question}>
            Are you deaf or do you have serious difficulty hearing?
          </div>
          <div>Radio</div>
          <div style={styles.question}>
            Are you blind or do you have serious difficulty seeing, even when
            wearing glasses?
          </div>
          <div>Radio</div>
          <div style={styles.question}>
            Because of a physical, cognitive, or emotional condition, do you
            have serious difficulty concentrating, remembering, or making
            decisions?
          </div>
          <div>Radio</div>
          <div style={styles.question}>
            Do you have serious difficulty walking or climbing stairs?
          </div>
          <div>Radio</div>
          <div style={styles.question}>
            Do you have difficulty dressing or bathing?
          </div>
          <div>Radio</div>
          <div style={styles.question}>
            Because of a physical, mental, or emotional condition, do you have
            difficulty doing errands alone such as visiting doctor's office or
            shopping?
          </div>
          <div>Radio</div>
          <div style={styles.question}>
            Do you have a physical, cognitive, and/or emotional condition that
            substantially inhibits one or more life activities not specified
            through the above questions, and want to share more? Please
            describe.
          </div>
          <div>Radio</div>
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
