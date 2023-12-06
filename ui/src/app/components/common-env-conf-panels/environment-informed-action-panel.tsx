import * as React from 'react';
import { CSSProperties } from 'react';

import { AppStatus, Profile, RuntimeStatus, Workspace } from 'generated/fetch';

import { cond } from '@terra-ui-packages/core-utils';
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

interface CostsDrawnFromProps {
  usingInitialCredits: boolean;
  userIsCreator: boolean;
  creatorFreeCreditsRemaining: number;
  billingAccountName: string;
}
const CostsDrawnFrom = ({
  usingInitialCredits,
  userIsCreator,
  creatorFreeCreditsRemaining,
  billingAccountName,
}: CostsDrawnFromProps) => {
  const remainingCredits =
    creatorFreeCreditsRemaining === null ? (
      <Spinner size={10} />
    ) : (
      formatUsd(creatorFreeCreditsRemaining)
    );

  return cond(
    [
      usingInitialCredits && userIsCreator,
      () => (
        <div style={styles.costsDrawnFrom}>
          Costs will draw from your remaining {remainingCredits} of free
          credits.
        </div>
      ),
    ],
    [
      usingInitialCredits && !userIsCreator,
      () => (
        <div style={styles.costsDrawnFrom}>
          Costs will draw from workspace creator's remaining {remainingCredits}{' '}
          of free credits.
        </div>
      ),
    ],
    [
      !usingInitialCredits,
      () => (
        <div style={styles.costsDrawnFrom}>
          Costs will be charged to billing account {billingAccountName}.
        </div>
      ),
    ]
  );
};

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
      <CostsDrawnFrom
        {...{ creatorFreeCreditsRemaining }}
        usingInitialCredits={isUsingFreeTierBillingAccount(workspace)}
        userIsCreator={profile.username === workspace.creator}
        billingAccountName={workspace.billingAccountName}
      />
    </FlexColumn>
  );
};
