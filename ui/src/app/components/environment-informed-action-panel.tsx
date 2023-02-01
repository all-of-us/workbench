import * as React from 'react';

import colors, { colorWithWhiteness } from 'app/styles/colors';
import { formatUsd } from 'app/utils/numbers';
import { isUsingFreeTierBillingAccount } from 'app/utils/workspace-utils';

import { FlexRow } from './flex';
import { StartStopEnvironmentButton } from './runtime-configuration-panel/start-stop-environment-button';
import { styles } from './runtime-configuration-panel/styles';
import { RuntimeCostEstimator } from './runtime-cost-estimator';
import { Spinner } from './spinners';

const CostInfo = ({
  creatorFreeCreditsRemaining,
  runtimeChanged,
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
          ...(runtimeChanged
            ? {
                backgroundColor: colorWithWhiteness(colors.warning, 0.9),
              }
            : {}),
        }}
      >
        <RuntimeCostEstimator {...{ analysisConfig }} />
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
}) => (
  <FlexRow style={styles.environmentInformedActionPanelWrapper}>
    <StartStopEnvironmentButton {...{ status, onPause, onResume, appType }} />
    <CostInfo
      {...{ analysisConfig }}
      runtimeChanged={false}
      currentUser={profile.username}
      workspace={workspace}
      creatorFreeCreditsRemaining={creatorFreeCreditsRemaining}
    />
  </FlexRow>
);
