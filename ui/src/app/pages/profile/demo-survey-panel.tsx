import * as React from 'react';

import { Button } from 'app/components/buttons';
import colors from 'app/styles/colors';
import { displayDateWithoutHours } from 'app/utils/dates';

import { styles } from './profile-styles';

interface Props {
  demographicSurveyCompletionTime: number;
  firstSignInTime: number;
  onClick: () => void;
}
export const DemographicSurveyPanel = (props: Props) => {
  const { demographicSurveyCompletionTime, firstSignInTime, onClick } = props;
  return (
    <div style={{ marginTop: '1rem', marginLeft: '1rem' }}>
      <div style={styles.title}>Optional Demographics Survey</div>
      <hr style={{ ...styles.verticalLine }} />
      <div style={{ color: colors.primary, fontSize: '14px' }}>
        <div>Survey Completed</div>
        {/* If a user has created an account, they have, by definition, completed the demographic survey*/}
        <div>
          {displayDateWithoutHours(
            demographicSurveyCompletionTime !== null
              ? demographicSurveyCompletionTime
              : firstSignInTime
          )}
        </div>
        <Button
          type={'link'}
          style={styles.updateSurveyButton}
          onClick={() => onClick()}
          data-test-id={'demographics-survey-button'}
        >
          Update Survey
        </Button>
      </div>
    </div>
  );
};
