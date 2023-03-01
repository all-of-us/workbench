import * as React from 'react';

import colors, { colorWithWhiteness } from 'app/styles/colors';
import { formatUsd } from 'app/utils/numbers';
import { isUsingFreeTierBillingAccount } from 'app/utils/workspace-utils';

import { EnvironmentCostEstimator } from './environment-cost-estimator';
import { FlexRow } from './flex';
import { StartStopEnvironmentButton } from './runtime-configuration-panel/start-stop-environment-button';
import { styles } from './runtime-configuration-panel/styles';
import { Spinner } from './spinners';

const CostInfo = ({
  creatorFreeCreditsRemaining,
  environmentChanged,
  analysisConfig,
  currentUser,
  workspace,
}) => {
  const remainingCredits =
    creatorFreeCreditsRemaining === null ? (
      <Spinner size={10} />
    ) : (
      formatUsd(creatorFreeCreditsRemaining)
    );

  return (
    <FlexRow data-test-id='cost-estimator'>
      <div
        style={{
          padding: '.495rem .75rem',
          ...(environmentChanged
            ? {
                backgroundColor: colorWithWhiteness(colors.warning, 0.9),
              }
            : {}),
        }}
      >
        <EnvironmentCostEstimator {...{ analysisConfig }} />
      </div>
      {isUsingFreeTierBillingAccount(workspace) &&
        currentUser === workspace.creator && (
          <div style={styles.costsDrawnFrom}>
            Costs will draw from your remaining {remainingCredits} of free
            credits.
          </div>
        )}
      {isUsingFreeTierBillingAccount(workspace) &&
        currentUser !== workspace.creator && (
          <div style={styles.costsDrawnFrom}>
            Costs will draw from workspace creator's remaining{' '}
            {remainingCredits} of free credits.
          </div>
        )}
      {!isUsingFreeTierBillingAccount(workspace) && (
        <div style={styles.costsDrawnFrom}>
          Costs will be charged to billing account{' '}
          {workspace.billingAccountName}.
        </div>
      )}
    </FlexRow>
  );
};

export const EnvironmentInformedActionPanel = ({
  creatorFreeCreditsRemaining,
  profile,
  workspace,
  analysisConfig,
  status,
  onPause,
  onResume,
  appType,
  environmentChanged = false,
}) => (
  <FlexRow style={styles.environmentInformedActionPanelWrapper}>
    <StartStopEnvironmentButton {...{ status, onPause, onResume, appType }} />
    <CostInfo
      {...{ analysisConfig, environmentChanged }}
      currentUser={profile.username}
      workspace={workspace}
      creatorFreeCreditsRemaining={creatorFreeCreditsRemaining}
    />
  </FlexRow>
);
