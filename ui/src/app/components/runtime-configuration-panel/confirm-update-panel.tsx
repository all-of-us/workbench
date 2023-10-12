import * as React from 'react';

import { Button } from 'app/components/buttons';
import { EnvironmentCostEstimator } from 'app/components/common-env-conf-panels/environment-cost-estimator';
import { styles } from 'app/components/common-env-conf-panels/styles';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { WarningMessage } from 'app/components/messages';
import { TextColumn } from 'app/components/text-column';
import {
  AnalysisConfig,
  diffsToUpdateMessaging,
  getAnalysisConfigDiffs,
} from 'app/utils/runtime-utils';

import { UpdateButton } from './update-button';

interface Props {
  existingAnalysisConfig: AnalysisConfig;
  newAnalysisConfig: AnalysisConfig;
  requestAnalysisConfig: (ac: AnalysisConfig) => void;
  runtimeCanBeCreated: boolean;
  runtimeCanBeUpdated: boolean;
  onCancel: () => void;
  onClose: () => void;
}
export const ConfirmUpdatePanel = ({
  existingAnalysisConfig,
  newAnalysisConfig,
  requestAnalysisConfig,
  runtimeCanBeCreated,
  runtimeCanBeUpdated,
  onCancel,
  onClose,
}: Props) => {
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
            marginTop: '.15rem',
            marginBottom: '.3rem',
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
        <FlexColumn style={{ gap: '8px', marginTop: '.75rem' }}>
          <div>
            <b style={{ fontSize: 10 }}>New estimated cost</b>
            <div
              style={{
                ...styles.environmentInformedActionPanelWrapper,
                ...styles.costComparison,
              }}
            >
              <EnvironmentCostEstimator
                analysisConfig={newAnalysisConfig}
                isGKEApp={false}
              />
            </div>
          </div>
          <div>
            <b style={{ fontSize: 10 }}>Previous estimated cost</b>
            <div
              style={{
                ...styles.environmentInformedActionPanelWrapper,
                ...styles.costComparison,
                color: 'grey',
                backgroundColor: '',
              }}
            >
              <EnvironmentCostEstimator
                analysisConfig={existingAnalysisConfig}
                isGKEApp={false}
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
              <div style={{ marginTop: '0.75rem' }}>
                {updateMessaging.warnMore}
              </div>
            </React.Fragment>
          </TextColumn>
        </WarningMessage>
      )}
      <FlexRow style={{ justifyContent: 'flex-end', marginTop: '1.125rem' }}>
        <Button
          type='secondary'
          aria-label='Cancel'
          style={{ marginRight: '.375rem' }}
          onClick={onCancel}
        >
          Cancel
        </Button>
        <UpdateButton
          {...{
            requestAnalysisConfig,
            runtimeCanBeCreated,
            onClose,
            runtimeCanBeUpdated,
            updateMessaging,
          }}
          analysisConfig={newAnalysisConfig}
        />
      </FlexRow>
    </React.Fragment>
  );
};
