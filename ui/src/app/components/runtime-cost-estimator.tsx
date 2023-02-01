import { CSSProperties } from 'react';

import { FlexColumn, FlexRow } from 'app/components/flex';
import { TooltipTrigger } from 'app/components/popups';
import colors from 'app/styles/colors';
import { cond, reactStyles } from 'app/utils';
import {
  detachableDiskPricePerMonth,
  diskConfigPricePerMonth,
  machineRunningCost,
  machineRunningCostBreakdown,
  machineStorageCost,
  machineStorageCostBreakdown,
} from 'app/utils/machines';
import { formatUsd } from 'app/utils/numbers';
import { AnalysisConfig } from 'app/utils/runtime-utils';

interface Props {
  analysisConfig: AnalysisConfig;
  costTextColor?: string;
  style?: CSSProperties;
}

const styles = reactStyles({
  costSection: {
    marginRight: '0.5rem',
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
  },
});

export const RuntimeCostEstimator = ({
  analysisConfig,
  costTextColor = colors.accent,
  style = {},
}: Props) => {
  const { detachedDisk, diskConfig } = analysisConfig;
  const runningCost = machineRunningCost(analysisConfig);
  const runningCostBreakdown = machineRunningCostBreakdown(analysisConfig);
  const storageCost = machineStorageCost(analysisConfig);
  const storageCostBreakdown = machineStorageCostBreakdown(analysisConfig);
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
    <FlexRow style={style}>
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
          <div style={costStyle} data-test-id='running-cost'>
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
              {storageCostBreakdown.map((lineItem, i) => (
                <div key={i}>{lineItem}</div>
              ))}
            </div>
          }
        >
          <div style={costStyle} data-test-id='storage-cost'>
            <div style={styles.costValue}>{formatUsd(storageCost)}</div>
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
