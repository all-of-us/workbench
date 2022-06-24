import * as React from 'react';

import { Button } from 'app/components/buttons';
import { displayDateWithoutHours } from 'app/utils/dates';
import { serverConfigStore } from 'app/utils/stores';

import { styles } from './profile-styles';

interface Props {
  demographicSurveyCompletionTime: number;
  firstSignInTime: number;
  onClick: () => void;
}
export const DemographicSurveyPanel = (props: Props) => {
  const { demographicSurveyCompletionTime, firstSignInTime, onClick } = props;
  const { enableUpdatedDemographicSurvey } = serverConfigStore.get().config;

  const surveyCompleted =
    (enableUpdatedDemographicSurvey &&
      demographicSurveyCompletionTime !== null) ||
    !enableUpdatedDemographicSurvey;

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
          <div>Survey Incomplete</div>
        )}
        <Button
          {...{ onClick }}
          type={'link'}
          style={styles.updateSurveyButton}
          data-test-id={'demographics-survey-button'}
        >
          {surveyCompleted ? 'Update Survey' : 'Complete Survey'}
        </Button>
      </div>
    </div>
  );
};
