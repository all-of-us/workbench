import { FlexColumn, FlexRow } from 'app/components/flex';
import { TooltipTrigger } from 'app/components/popups';
import colors from 'app/styles/colors';
import { reactStyles } from 'app/utils';
import {
  ComputeType,
  diskPricePerMonth,
  machineRunningCost,
  machineRunningCostBreakdown,
  machineStorageCost,
  machineStorageCostBreakdown,
} from 'app/utils/machines';
import { formatUsd } from 'app/utils/numbers';
import { RuntimeConfig } from 'app/utils/runtime-utils';
import { CSSProperties } from 'react';

interface Props {
  runtimeConfig: RuntimeConfig;
  costTextColor?: string;
  style?: CSSProperties;
}

const styles = reactStyles({
  costSection: {
    marginRight: '1rem',
    overflow: 'hidden',
    whiteSpace: 'nowrap',
  },
  cost: {
    overflow: 'hidden',
    textOverflow: 'ellipsis',
  },
});

export const RuntimeCostEstimator = ({
  runtimeConfig,
  costTextColor = colors.accent,
  style = {},
}: Props) => {
  const { computeType, diskConfig } = runtimeConfig;
  const runningCost = machineRunningCost(runtimeConfig);
  const runningCostBreakdown = machineRunningCostBreakdown(runtimeConfig);
  const storageCost = machineStorageCost(runtimeConfig);
  const storageCostBreakdown = machineStorageCostBreakdown(runtimeConfig);
  const costStyle = {
    ...styles.cost,
    fontSize: diskConfig.detachable ? '12px' : '15px',
    color: costTextColor,
  };
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
            {formatUsd(runningCost)}/hour
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
            {formatUsd(storageCost)}/hour
          </div>
        </TooltipTrigger>
      </FlexColumn>
      {diskConfig.detachable && computeType === ComputeType.Standard && (
        <FlexColumn style={styles.costSection}>
          <div style={{ fontSize: '10px', fontWeight: 600 }}>
            Persistent disk cost
          </div>
          <div style={costStyle} data-test-id='pd-cost'>
            {formatUsd(diskConfig.size * diskPricePerMonth)}/month
          </div>
        </FlexColumn>
      )}
    </FlexRow>
  );
};
