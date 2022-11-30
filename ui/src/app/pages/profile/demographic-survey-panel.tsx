import * as React from 'react';

import { ButtonWithLocationState } from 'app/components/buttons';
import { DEMOGRAPHIC_SURVEY_V2_PATH } from 'app/utils/constants';
import { displayDateWithoutHours } from 'app/utils/dates';

import { styles } from './profile-styles';

interface Props {
  demographicSurveyCompletionTime: number;
  firstSignInTime: number;
  onClick: () => void;
}
export const DemographicSurveyPanel = (props: Props) => {
  const { demographicSurveyCompletionTime, firstSignInTime } = props;

  const surveyCompleted = demographicSurveyCompletionTime !== null;

  return (
    <div style={styles.panel}>
      <div style={styles.title}>Demographics Survey</div>
      <hr style={{ ...styles.verticalLine }} />
      <div style={styles.panelBody}>
        {surveyCompleted ? (
          <>
            <div>Survey Completed</div>
            <div>
              {displayDateWithoutHours(
                demographicSurveyCompletionTime ?? firstSignInTime
              )}
            </div>
          </>
        ) : (
          <div>Updated Survey Incomplete</div>
        )}
        <ButtonWithLocationState
          path={DEMOGRAPHIC_SURVEY_V2_PATH}
          type={'link'}
          style={styles.updateSurveyButton}
          data-test-id={'demographics-survey-button'}
        >
          {surveyCompleted ? 'Update Survey' : 'Complete Survey'}
        </ButtonWithLocationState>
      </div>
    </div>
  );
};
