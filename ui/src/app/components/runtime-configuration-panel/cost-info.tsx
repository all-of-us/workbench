import * as React from 'react';

import { FlexRow } from 'app/components/flex';
import { styles } from 'app/components/runtime-configuration-panel/styles';
import { RuntimeCostEstimator } from 'app/components/runtime-cost-estimator';
import { Spinner } from 'app/components/spinners';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import { formatUsd } from 'app/utils/numbers';
import { isUsingFreeTierBillingAccount } from 'app/utils/workspace-utils';

export const CostInfo = ({
  runtimeChanged,
  analysisConfig,
  currentUser,
  workspace,
  creatorFreeCreditsRemaining,
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
          padding: '.33rem .5rem',
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
