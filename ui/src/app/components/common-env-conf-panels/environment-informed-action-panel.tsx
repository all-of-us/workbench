import * as React from 'react';

import { AppStatus, Profile, RuntimeStatus, Workspace } from 'generated/fetch';

import { UIAppType } from 'app/components/apps-panel/utils';
import { FlexRow } from 'app/components/flex';
import { Spinner } from 'app/components/spinners';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import { formatUsd } from 'app/utils/numbers';
import { AnalysisConfig } from 'app/utils/runtime-utils';
import { isUsingFreeTierBillingAccount } from 'app/utils/workspace-utils';

import { EnvironmentCostEstimator } from './environment-cost-estimator';
import { StartStopEnvironmentButton } from './start-stop-environment-button';
import { styles } from './styles';

interface CostInfoProps {
  creatorFreeCreditsRemaining: number;
  environmentChanged: boolean;
  analysisConfig: AnalysisConfig;
  currentUser: string;
  workspace: Workspace;
  isGKEApp: boolean;
}
const CostInfo = ({
  creatorFreeCreditsRemaining,
  environmentChanged,
  analysisConfig,
  currentUser,
  workspace,
  isGKEApp,
}: CostInfoProps) => {
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
        <EnvironmentCostEstimator {...{ analysisConfig }} isGKEApp={isGKEApp} />
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

interface Props {
  creatorFreeCreditsRemaining: number;
  profile: Profile;
  workspace: Workspace;
  analysisConfig: AnalysisConfig;
  status: AppStatus | RuntimeStatus;
  onPause: () => void;
  onResume: () => void;
  appType: UIAppType;
  environmentChanged?: boolean;
}
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
}: Props) => (
  <FlexRow style={styles.environmentInformedActionPanelWrapper}>
    {appType === UIAppType.JUPYTER && (
      <StartStopEnvironmentButton {...{ status, onPause, onResume, appType }} />
    )}
    <CostInfo
      {...{ analysisConfig, environmentChanged }}
      currentUser={profile.username}
      workspace={workspace}
      creatorFreeCreditsRemaining={creatorFreeCreditsRemaining}
      isGKEApp={appType !== UIAppType.JUPYTER}
    />
  </FlexRow>
);
