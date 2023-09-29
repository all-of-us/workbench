import * as React from 'react';

import { ButtonWithLocationState } from 'app/components/buttons';
import { DEMOGRAPHIC_SURVEY_V2_PATH } from 'app/utils/constants';
import { displayDateWithoutHours } from 'app/utils/dates';

import { styles } from './profile-styles';

interface Props {
  demographicSurveyCompletionTime: number;
}
export const DemographicSurveyPanel = (props: Props) => {
  const { demographicSurveyCompletionTime } = props;

  return (
    <div style={styles.panel} data-test-id='demographic-survey'>
      <div style={styles.title}>Demographics Survey</div>
      <hr style={{ ...styles.verticalLine }} />
      <div style={styles.panelBody}>
        {demographicSurveyCompletionTime ? (
          <>
            <div>Survey Completed</div>
            <div>
              {displayDateWithoutHours(demographicSurveyCompletionTime)}
            </div>
          </>
        ) : (
          <div>Updated Survey Incomplete</div>
        )}
        <ButtonWithLocationState
          path={DEMOGRAPHIC_SURVEY_V2_PATH}
          type='link'
          style={styles.updateSurveyButton}
          data-test-id='demographics-survey-button'
        >
          {demographicSurveyCompletionTime
            ? 'Update Survey'
            : 'Complete Survey'}
        </ButtonWithLocationState>
      </div>
    </div>
  );
};
