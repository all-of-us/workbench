import { CSSProperties } from 'react';

import { FlexColumn, FlexRow } from 'app/components/flex';
import { TooltipTrigger } from 'app/components/popups';
import colors from 'app/styles/colors';
import { reactStyles } from 'app/utils';
import { AnalysisConfig } from 'app/utils/analysis-config';
import {
  derivePdFromAnalysisConfig,
  machineRunningCostBreakdown,
  machineRunningCostPerHour,
  machineStorageCostBreakdown,
  machineStorageCostPerHour,
  persistentDiskPricePerMonth,
  RunningCost,
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
  const { computeType, gpuConfig, machine, dataprocConfig } = analysisConfig;

  // temp derive from analysisConfig
  const persistentDisk = derivePdFromAnalysisConfig(analysisConfig);

  const runningCostParams: RunningCost = {
    dataprocConfig,
    persistentDisk,
    computeType,
    gpuConfig,
    machine,
  };
  const runningCost =
    machineRunningCostPerHour(runningCostParams) +
    (isGKEApp ? GKE_APP_HOURLY_USD_COST_PER_WORKSPACE : 0);
  const runningCostBreakdown = machineRunningCostBreakdown(runningCostParams);
  const pausedCost =
    machineStorageCostPerHour({ dataprocConfig, persistentDisk }) +
    (isGKEApp ? GKE_APP_HOURLY_USD_COST_PER_WORKSPACE : 0);
  const pausedCostBreakdown = machineStorageCostBreakdown({
    dataprocConfig,
    persistentDisk,
  });

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
  const pdCost = persistentDisk
    ? persistentDiskPricePerMonth(persistentDisk)
    : 0;
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
        {!isGKEApp && (
          <div>
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
          </div>
        )}
      </FlexColumn>
      {/* Always show PD Cost for GKE apps; hide it for Jupyter if disk size is zero*/}
      {(!!pdCost || isGKEApp) && (
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
