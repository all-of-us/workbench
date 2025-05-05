import * as React from 'react';
import { CSSProperties } from 'react';

import { AppStatus, Profile, RuntimeStatus, Workspace } from 'generated/fetch';

import { cond } from '@terra-ui-packages/core-utils';
import { UIAppType } from 'app/components/apps-panel/utils';
import { EnvironmentCostEstimator } from 'app/components/common-env-conf-panels/environment-cost-estimator';
import { StartStopEnvironmentButton } from 'app/components/common-env-conf-panels/start-stop-environment-button';
import { styles } from 'app/components/common-env-conf-panels/styles';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { Spinner } from 'app/components/spinners';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import { AnalysisConfig } from 'app/utils/analysis-config';
import { formatUsd } from 'app/utils/numbers';
import { BILLING_ACCOUNT_DISABLED_TOOLTIP } from 'app/utils/strings';
import {
  isUsingInitialCredits,
  isValidBilling,
} from 'app/utils/workspace-utils';

interface CostsDrawnFromProps {
  usingInitialCredits: boolean;
  userIsCreator: boolean;
  creatorInitialCreditsRemaining: number;
  billingAccountName: string;
  style?: CSSProperties;
}
const CostsDrawnFrom = ({
  usingInitialCredits,
  userIsCreator,
  creatorInitialCreditsRemaining,
  billingAccountName,
  style,
}: CostsDrawnFromProps) => {
  const remainingCredits =
    creatorInitialCreditsRemaining === null ? (
      <Spinner size={10} />
    ) : (
      formatUsd(creatorInitialCreditsRemaining)
    );

  return cond(
    [
      usingInitialCredits && userIsCreator,
      () => (
        <div style={{ ...styles.costsDrawnFrom, ...style }}>
          Costs will draw from your remaining {remainingCredits} of free
          credits.
        </div>
      ),
    ],
    [
      usingInitialCredits && !userIsCreator,
      () => (
        <div style={{ ...styles.costsDrawnFrom, ...style }}>
          Costs will draw from the workspace creator's remaining{' '}
          {remainingCredits} of free credits.
        </div>
      ),
    ],
    [
      !usingInitialCredits,
      () => (
        <div style={{ ...styles.costsDrawnFrom, ...style }}>
          Costs will be charged to billing account {billingAccountName}.
        </div>
      ),
    ]
  );
};

interface PanelProps {
  creatorInitialCreditsRemaining: number;
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
  creatorInitialCreditsRemaining,
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
    justifyContent: 'space-evenly',
    flexGrow: 1,
    ...(environmentChanged
      ? {
          backgroundColor: colorWithWhiteness(colors.warning, 0.9),
        }
      : {}),
  };

  // with a PD, the "drawn from" text is too crowded
  const showCostsDrawnFromWithCosts = !analysisConfig.diskConfig.detachable;

  return (
    <FlexColumn>
      <FlexRow style={styles.environmentInformedActionPanelWrapper}>
        {appType === UIAppType.JUPYTER && (
          <StartStopEnvironmentButton
            {...{ status, appType, onPause, onResume }}
            disabled={!isValidBilling(workspace)}
            disabledTooltip={BILLING_ACCOUNT_DISABLED_TOOLTIP}
          />
        )}
        <EnvironmentCostEstimator
          {...{ analysisConfig }}
          data-test-id='cost-estimator'
          isGKEApp={appType !== UIAppType.JUPYTER}
          style={costEstimatorStyle}
        />
        {showCostsDrawnFromWithCosts && (
          <CostsDrawnFrom
            {...{ creatorInitialCreditsRemaining }}
            usingInitialCredits={isUsingInitialCredits(workspace)}
            userIsCreator={profile.username === workspace.creatorUser.userName}
            billingAccountName={workspace.billingAccountName}
            style={{
              borderLeft: `1px solid ${colorWithWhiteness(colors.dark, 0.5)}`,
              paddingLeft: '.5rem',
            }}
          />
        )}
      </FlexRow>
      {!showCostsDrawnFromWithCosts && (
        <CostsDrawnFrom
          {...{ creatorInitialCreditsRemaining }}
          usingInitialCredits={isUsingInitialCredits(workspace)}
          userIsCreator={profile.username === workspace.creatorUser.userName}
          billingAccountName={workspace.billingAccountName}
        />
      )}
    </FlexColumn>
  );
};
