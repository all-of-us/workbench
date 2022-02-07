import { FlexColumn, FlexRow } from 'app/components/flex';
import { TooltipTrigger } from 'app/components/popups';
import colors from 'app/styles/colors';
import { reactStyles } from 'app/utils';
import {
  ComputeType,
  diskConfigPrice,
  machineRunningCost,
  machineRunningCostBreakdown,
  machineStorageCost,
  machineStorageCostBreakdown,
} from 'app/utils/machines';
import { formatUsd } from 'app/utils/numbers';
import { AnalysisConfig } from 'app/utils/runtime-utils';
import { CSSProperties } from 'react';

interface Props {
  analysisConfig: AnalysisConfig;
  costTextColor?: string;
  style?: CSSProperties;
}

const styles = reactStyles({
  costSection: {
    marginRight: '1rem',
    overflow: 'hidden',
  },
  cost: {
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
  },
});

export const RuntimeCostEstimator = ({
  analysisConfig,
  costTextColor = colors.accent,
  style = {},
}: Props) => {
  const { computeType, diskConfig } = analysisConfig;
  const runningCost = machineRunningCost(analysisConfig);
  const runningCostBreakdown = machineRunningCostBreakdown(analysisConfig);
  const storageCost = machineStorageCost(analysisConfig);
  const storageCostBreakdown = machineStorageCostBreakdown(analysisConfig);
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
            {formatUsd(diskConfigPrice(diskConfig))}/month
          </div>
        </FlexColumn>
      )}
    </FlexRow>
  );
};
