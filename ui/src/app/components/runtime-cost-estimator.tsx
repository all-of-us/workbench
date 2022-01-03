import {FlexColumn, FlexRow} from 'app/components/flex';
import {TooltipTrigger} from 'app/components/popups';
import colors from 'app/styles/colors';
import {
  ComputeType,
  diskPricePerMonth,
  findGpu,
  findMachineByName,
  machineRunningCost,
  machineRunningCostBreakdown,
  machineStorageCost,
  machineStorageCostBreakdown,
} from 'app/utils/machines';
import {formatUsd} from 'app/utils/numbers';
import { RuntimeConfig } from 'app/utils/runtime-utils';
import { CSSProperties } from 'react';


interface Props {
  runtimeParameters: RuntimeConfig;
  // TODO(RW-7582): remove this prop, this information should be self-contained
  // by the RuntimeConfig instead.
  usePersistentDisk: boolean;
  costTextColor?: string;
  style?: CSSProperties;
}

export const RuntimeCostEstimator = ({
  runtimeParameters,
  usePersistentDisk,
  costTextColor = colors.accent,
  style = {}
}: Props) => {
  const {
    computeType,
    diskSize,
    pdSize,
    machine,
    gpuConfig,
    dataprocConfig
  } = runtimeParameters;
  const {
    numberOfWorkers = 0,
    workerMachineType = null,
    workerDiskSize = null,
    numberOfPreemptibleWorkers = 0
  } = dataprocConfig || {};
  const workerMachine = findMachineByName(workerMachineType);
  const gpu = gpuConfig ? findGpu(gpuConfig.gpuType, gpuConfig.numOfGpus) : null;
  const costConfig = {
    computeType, masterMachine: machine, gpu,
    masterDiskSize: usePersistentDisk ? pdSize : diskSize,
    numberOfWorkers, numberOfPreemptibleWorkers, workerDiskSize, workerMachine
  };
  const runningCost = machineRunningCost(costConfig);
  const runningCostBreakdown = machineRunningCostBreakdown(costConfig);
  const storageCost = machineStorageCost(costConfig);
  const storageCostBreakdown = machineStorageCostBreakdown(costConfig);
  const costPriceFontSize = usePersistentDisk ? '12px' : '20px';
  return <FlexRow style={style}>
    <FlexColumn style={{marginRight: '1rem'}}>
      <div style={{fontSize: '10px', fontWeight: 600}}>Cost when running</div>
      <TooltipTrigger content={
        <div>
          <div>Cost Breakdown</div>
          {runningCostBreakdown.map((lineItem, i) => <div key={i}>{lineItem}</div>)}
        </div>
      }>
        <div
          style={{fontSize: costPriceFontSize, color: costTextColor}}
          data-test-id='running-cost'
        >
          {formatUsd(runningCost)}/hour
        </div>
      </TooltipTrigger>
    </FlexColumn>
    <FlexColumn style={{marginRight: '1rem'}}>
      <div style={{fontSize: '10px', fontWeight: 600}}>Cost when paused</div>
      <TooltipTrigger content={
        <div>
          <div>Cost Breakdown</div>
          {storageCostBreakdown.map((lineItem, i) => <div key={i}>{lineItem}</div>)}
        </div>
      }>
        <div
          style={{fontSize: costPriceFontSize, color: costTextColor}}
          data-test-id='storage-cost'
        >
          {formatUsd(storageCost)}/hour
        </div>
      </TooltipTrigger>
    </FlexColumn>
    {usePersistentDisk && computeType === ComputeType.Standard && <FlexColumn>
      <div style={{fontSize: '10px', fontWeight: 600}}>Persistent disk cost</div>
      <div
        style={{fontSize: costPriceFontSize, color: costTextColor}}
        data-test-id='pd-cost'
      >
        {formatUsd(pdSize * diskPricePerMonth)}/month
      </div>
    </FlexColumn>}
  </FlexRow>;
};
