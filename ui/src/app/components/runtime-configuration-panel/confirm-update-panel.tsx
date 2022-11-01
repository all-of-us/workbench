import * as React from 'react';

import { Button } from 'app/components/buttons';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { WarningMessage } from 'app/components/messages';
import { styles } from 'app/components/runtime-configuration-panel/styles';
import { RuntimeCostEstimator } from 'app/components/runtime-cost-estimator';
import { TextColumn } from 'app/components/text-column';
import {
  diffsToUpdateMessaging,
  getAnalysisConfigDiffs,
} from 'app/utils/runtime-utils';

export const ConfirmUpdatePanel = ({
  existingAnalysisConfig,
  newAnalysisConfig,
  onCancel,
  updateButton,
}) => {
  const configDiffs = getAnalysisConfigDiffs(
    existingAnalysisConfig,
    newAnalysisConfig
  );
  const updateMessaging = diffsToUpdateMessaging(configDiffs);
  return (
    <React.Fragment>
      <div style={styles.controlSection}>
        <h3
          style={{
            ...styles.baseHeader,
            ...styles.sectionHeader,
            marginTop: '.1rem',
            marginBottom: '.2rem',
          }}
        >
          Editing your environment
        </h3>
        <div>
          You're about to apply the following changes to your environment:
        </div>
        <ul>
          {configDiffs.map((diff, i) => (
            <li key={i}>
              {diff.desc} from <b>{diff.previous}</b> to <b>{diff.new}</b>
            </li>
          ))}
        </ul>
        <FlexColumn style={{ gap: '8px', marginTop: '.5rem' }}>
          <div>
            <b style={{ fontSize: 10 }}>New estimated cost</b>
            <div
              style={{
                ...styles.costPredictorWrapper,
                ...styles.costComparison,
              }}
            >
              <RuntimeCostEstimator analysisConfig={newAnalysisConfig} />
            </div>
          </div>
          <div>
            <b style={{ fontSize: 10 }}>Previous estimated cost</b>
            <div
              style={{
                ...styles.costPredictorWrapper,
                ...styles.costComparison,
                color: 'grey',
                backgroundColor: '',
              }}
            >
              <RuntimeCostEstimator
                analysisConfig={existingAnalysisConfig}
                costTextColor='grey'
              />
            </div>
          </div>
        </FlexColumn>
      </div>
      {updateMessaging.warn && (
        <WarningMessage iconSize={30} iconPosition={'center'}>
          <TextColumn>
            <React.Fragment>
              <div>{updateMessaging.warn}</div>
              <div style={{ marginTop: '0.5rem' }}>
                {updateMessaging.warnMore}
              </div>
            </React.Fragment>
          </TextColumn>
        </WarningMessage>
      )}
      <FlexRow style={{ justifyContent: 'flex-end', marginTop: '.75rem' }}>
        <Button
          type='secondary'
          aria-label='Cancel'
          style={{ marginRight: '.25rem' }}
          onClick={onCancel}
        >
          Cancel
        </Button>
        {updateButton}
      </FlexRow>
    </React.Fragment>
  );
};
