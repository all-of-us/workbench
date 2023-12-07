import { CSSProperties } from 'react';

import { cond } from '@terra-ui-packages/core-utils';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { TooltipTrigger } from 'app/components/popups';
import colors from 'app/styles/colors';
import { reactStyles } from 'app/utils';
import { AnalysisConfig } from 'app/utils/analysis-config';
import {
  detachableDiskPricePerMonth,
  diskConfigPricePerMonth,
  machineRunningCost,
  machineRunningCostBreakdown,
  machineStorageCost,
  machineStorageCostBreakdown,
} from 'app/utils/machines';
import { formatUsd } from 'app/utils/numbers';

interface Props {
  analysisConfig: AnalysisConfig;
  isGKEApp: boolean;
  costTextColor?: string;
  style?: CSSProperties;
}

const styles = reactStyles({
  costSection: {
    marginRight: '1rem',
    overflow: 'hidden',
  },
  cost: {
    fontSize: '12px',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
    display: 'flex',
  },
  costValue: {
    fontSize: '1.25rem',
    fontWeight: '600',
  },
  costPeriod: {
    fontWeight: '600',
    marginLeft: '.3rem',
  },
});

// To simplify calculations and the UI, we assume the workspace has only one GKE app running.
const GKE_APP_HOURLY_USD_COST_PER_WORKSPACE = 0.2;

export const EnvironmentCostEstimator = ({
  analysisConfig,
  isGKEApp,
  costTextColor = colors.accent,
  style,
}: Props) => {
  const { detachedDisk, diskConfig } = analysisConfig;
  const runningCost =
    machineRunningCost(analysisConfig) +
    (isGKEApp ? GKE_APP_HOURLY_USD_COST_PER_WORKSPACE : 0);
  const runningCostBreakdown = machineRunningCostBreakdown(analysisConfig);
  const pausedCost =
    machineStorageCost(analysisConfig) +
    (isGKEApp ? GKE_APP_HOURLY_USD_COST_PER_WORKSPACE : 0);
  const pausedCostBreakdown = machineStorageCostBreakdown(analysisConfig);

  if (isGKEApp) {
    const gkeBaseCost = `${formatUsd(
      GKE_APP_HOURLY_USD_COST_PER_WORKSPACE
    )}/hr base environment cost`;
    runningCostBreakdown.push(gkeBaseCost);
    pausedCostBreakdown.push(gkeBaseCost);
  }

  const costStyle = {
    ...styles.cost,
    color: costTextColor,
  };
  const pdCost = cond(
    [diskConfig.detachable, () => diskConfigPricePerMonth(diskConfig)],
    [!!detachedDisk, () => detachableDiskPricePerMonth(detachedDisk)],
    () => 0
  );
  return (
    <FlexRow {...{ style }}>
      <FlexColumn style={styles.costSection}>
        <div style={{ fontSize: '10px', fontWeight: 600 }}>
          Cost when running
        </div>
        <TooltipTrigger
          content={
            <div>
              <div>Cost Breakdown</div>
              {runningCostBreakdown.map((lineItem, i) => (
                <div key={i}>{lineItem}</div>
              ))}
            </div>
          }
        >
          <div
            style={costStyle}
            data-test-id='running-cost'
            aria-label='cost while running'
          >
            <div style={styles.costValue}>{formatUsd(runningCost)}</div>
            <div style={styles.costPeriod}>{` per hour`}</div>
          </div>
        </TooltipTrigger>
      </FlexColumn>
      <FlexColumn style={styles.costSection}>
        <div style={{ fontSize: '10px', fontWeight: 600 }}>
          Cost when paused
        </div>
        <TooltipTrigger
          content={
            <div>
              <div>Cost Breakdown</div>
              {pausedCostBreakdown.map((lineItem, i) => (
                <div key={i}>{lineItem}</div>
              ))}
            </div>
          }
        >
          <div
            style={costStyle}
            data-test-id='paused-cost'
            aria-label='cost while paused'
          >
            <div style={styles.costValue}>{formatUsd(pausedCost)}</div>
            <div style={styles.costPeriod}>{` per hour`}</div>
          </div>
        </TooltipTrigger>
      </FlexColumn>
      {!!pdCost && (
        <FlexColumn style={styles.costSection}>
          <div style={{ fontSize: '10px', fontWeight: 600 }}>
            Persistent disk cost
          </div>
          <div style={costStyle} data-test-id='pd-cost'>
            <div style={styles.costValue}>{formatUsd(pdCost)}</div>
            <div style={styles.costPeriod}>{` per month`}</div>
          </div>
        </FlexColumn>
      )}
    </FlexRow>
  );
};
