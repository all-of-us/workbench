import * as React from 'react';
import { CSSProperties } from 'react';

import { AppStatus, Profile, RuntimeStatus, Workspace } from 'generated/fetch';

import { UIAppType } from 'app/components/apps-panel/utils';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { Spinner } from 'app/components/spinners';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import { AnalysisConfig } from 'app/utils/analysis-config';
import { formatUsd } from 'app/utils/numbers';
import { isUsingFreeTierBillingAccount } from 'app/utils/workspace-utils';

import { EnvironmentCostEstimator } from './environment-cost-estimator';
import { StartStopEnvironmentButton } from './start-stop-environment-button';
import { styles } from './styles';

interface PanelProps {
  creatorFreeCreditsRemaining: number;
  profile: Profile;
  workspace: Workspace;
  analysisConfig: AnalysisConfig;
  status: AppStatus | RuntimeStatus;
  appType: UIAppType;
  onPause?: () => void;
  onResume?: () => void;
  environmentChanged?: boolean;
}
export const EnvironmentInformedActionPanel = ({
  creatorFreeCreditsRemaining,
  profile,
  workspace,
  analysisConfig,
  status,
  appType,
  onPause,
  onResume,
  environmentChanged = false,
}: PanelProps) => {
  const remainingCredits =
    creatorFreeCreditsRemaining === null ? (
      <Spinner size={10} />
    ) : (
      formatUsd(creatorFreeCreditsRemaining)
    );

  const costEstimatorStyle: CSSProperties = {
    padding: '.495rem .75rem',
    ...(environmentChanged
      ? {
          backgroundColor: colorWithWhiteness(colors.warning, 0.9),
        }
      : {}),
  };

  return (
    <FlexColumn>
      <FlexRow style={styles.environmentInformedActionPanelWrapper}>
        {appType === UIAppType.JUPYTER && (
          <StartStopEnvironmentButton
            {...{ status, appType, onPause, onResume }}
          />
        )}
        <FlexRow data-test-id='cost-estimator' style={costEstimatorStyle}>
          <EnvironmentCostEstimator
            {...{ analysisConfig }}
            isGKEApp={appType !== UIAppType.JUPYTER}
          />
        </FlexRow>
      </FlexRow>
      {isUsingFreeTierBillingAccount(workspace) &&
        profile.username === workspace.creator && (
          <div style={styles.costsDrawnFrom}>
            Costs will draw from your remaining {remainingCredits} of free
            credits.
          </div>
        )}
      {isUsingFreeTierBillingAccount(workspace) &&
        profile.username !== workspace.creator && (
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
    </FlexColumn>
  );
};
