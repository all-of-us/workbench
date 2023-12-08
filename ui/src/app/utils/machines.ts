import * as fp from 'lodash/fp';

import { Disk, DiskType } from 'generated/fetch';

import { DEFAULT, switchCase } from '@terra-ui-packages/core-utils';

import { AnalysisConfig } from './analysis-config';
import { formatUsd } from './numbers';
import { DiskConfig } from './runtime-utils';

// Copied from https://github.com/DataBiosphere/terra-ui/blob/219b063b07d56499ccc38013fd88f4f0b88f8cd6/src/data/machines.js

// The minimum size for DATAPROC is a recommendation from LEO team
// https://terra-bio.slack.com/archives/C9C4KKU65/p1658339782608629
export const MIN_DISK_SIZE_GB = 120;
export const DATAPROC_MIN_DISK_SIZE_GB = 150;

export enum ComputeType {
  Standard = 'Standard VM',
  Dataproc = 'Dataproc Cluster',
}

export const AutopauseMinuteThresholds = new Map([
  [30, '30 minutes (default)'],
  [60 * 8, '8 hours'],
  [5 * 24 * 60, '5 days'],
  [10 * 24 * 60, '10 days'],
]);

export const DEFAULT_AUTOPAUSE_THRESHOLD_MINUTES = 30;

export interface Machine {
  name: string;
  cpu: number;
  memory: number;
  price: number;
  preemptiblePrice: number;
}

const machineBases: Machine[] = [
  {
    name: 'n1-standard-1',
    cpu: 1,
    memory: 3.75,
    price: 0.0475,
    preemptiblePrice: 0.01,
  },
  {
    name: 'n1-standard-2',
    cpu: 2,
    memory: 7.5,
    price: 0.095,
    preemptiblePrice: 0.02,
  },
  {
    name: 'n1-standard-4',
    cpu: 4,
    memory: 15,
    price: 0.19,
    preemptiblePrice: 0.04,
  },
  {
    name: 'n1-standard-8',
    cpu: 8,
    memory: 30,
    price: 0.38,
    preemptiblePrice: 0.08,
  },
  {
    name: 'n1-standard-16',
    cpu: 16,
    memory: 60,
    price: 0.76,
    preemptiblePrice: 0.16,
  },
  {
    name: 'n1-standard-32',
    cpu: 32,
    memory: 120,
    price: 1.52,
    preemptiblePrice: 0.32,
  },
  {
    name: 'n1-standard-64',
    cpu: 64,
    memory: 240,
    price: 3.04,
    preemptiblePrice: 0.64,
  },
  {
    name: 'n1-standard-96',
    cpu: 96,
    memory: 360,
    price: 4.56,
    preemptiblePrice: 0.96,
  },
  {
    name: 'n1-highmem-2',
    cpu: 2,
    memory: 13,
    price: 0.1184,
    preemptiblePrice: 0.025,
  },
  {
    name: 'n1-highmem-4',
    cpu: 4,
    memory: 26,
    price: 0.2368,
    preemptiblePrice: 0.05,
  },
  {
    name: 'n1-highmem-8',
    cpu: 8,
    memory: 52,
    price: 0.4736,
    preemptiblePrice: 0.1,
  },
  {
    name: 'n1-highmem-16',
    cpu: 16,
    memory: 104,
    price: 0.9472,
    preemptiblePrice: 0.2,
  },
  {
    name: 'n1-highmem-32',
    cpu: 32,
    memory: 208,
    price: 1.8944,
    preemptiblePrice: 0.4,
  },
  {
    name: 'n1-highmem-64',
    cpu: 64,
    memory: 416,
    price: 3.7888,
    preemptiblePrice: 0.8,
  },
  {
    name: 'n1-highmem-96',
    cpu: 96,
    memory: 624,
    price: 5.6832,
    preemptiblePrice: 1.2,
  },
  {
    name: 'n1-highcpu-2',
    cpu: 2,
    memory: 1.8,
    price: 0.0709,
    preemptiblePrice: 0.015,
  },
  {
    name: 'n1-highcpu-4',
    cpu: 4,
    memory: 3.6,
    price: 0.1418,
    preemptiblePrice: 0.03,
  },
  {
    name: 'n1-highcpu-8',
    cpu: 8,
    memory: 7.2,
    price: 0.2836,
    preemptiblePrice: 0.06,
  },
  {
    name: 'n1-highcpu-16',
    cpu: 16,
    memory: 14.4,
    price: 0.5672,
    preemptiblePrice: 0.12,
  },
  {
    name: 'n1-highcpu-32',
    cpu: 32,
    memory: 28.8,
    price: 1.1344,
    preemptiblePrice: 0.24,
  },
  {
    name: 'n1-highcpu-64',
    cpu: 64,
    memory: 57.6,
    price: 2.2688,
    preemptiblePrice: 0.48,
  },
  {
    name: 'n1-highcpu-96',
    cpu: 96,
    memory: 86.4,
    price: 3.402,
    preemptiblePrice: 0.72,
  },
];

// As of June 21, 2021:
// GPUs are only supported with general-purpose N1 or accelerator-optimized A2 machine types.
// (https://cloud.google.com/compute/docs/gpus#restrictions)
// Instances with GPUs also have limitations on maximum number of CPUs and memory they can have.
// (https://cloud.google.com/compute/docs/gpus#other_available_nvidia_gpu_models)
// NVIDIA Tesla P100 is not available within the zone 'us-central1-a`.
// (https://cloud.google.com/compute/docs/gpus/gpu-regions-zones)
// The limitations don't vary perfectly linearly so it seemed easier and less brittle to enumerate them.
// Prices below are hourly and per GPU (https://cloud.google.com/compute/gpus-pricing).
export const gpuTypes = [
  {
    name: 'NVIDIA Tesla T4',
    type: 'nvidia-tesla-t4',
    numGpus: 1,
    maxNumCpus: 24,
    maxMem: 156,
    price: 0.35,
    preemptiblePrice: 0.11,
  },
  {
    name: 'NVIDIA Tesla T4',
    type: 'nvidia-tesla-t4',
    numGpus: 2,
    maxNumCpus: 48,
    maxMem: 312,
    price: 0.7,
    preemptiblePrice: 0.22,
  },
  {
    name: 'NVIDIA Tesla T4',
    type: 'nvidia-tesla-t4',
    numGpus: 4,
    maxNumCpus: 96,
    maxMem: 624,
    price: 1.4,
    preemptiblePrice: 0.44,
  },
  {
    name: 'NVIDIA Tesla K80',
    type: 'nvidia-tesla-k80',
    numGpus: 1,
    maxNumCpus: 8,
    maxMem: 52,
    price: 0.45,
    preemptiblePrice: 0.135,
  },
  {
    name: 'NVIDIA Tesla K80',
    type: 'nvidia-tesla-k80',
    numGpus: 2,
    maxNumCpus: 16,
    maxMem: 104,
    price: 0.9,
    preemptiblePrice: 0.27,
  },
  {
    name: 'NVIDIA Tesla K80',
    type: 'nvidia-tesla-k80',
    numGpus: 4,
    maxNumCpus: 32,
    maxMem: 208,
    price: 1.35,
    preemptiblePrice: 0.54,
  },
  {
    name: 'NVIDIA Tesla K80',
    type: 'nvidia-tesla-k80',
    numGpus: 8,
    maxNumCpus: 64,
    maxMem: 208,
    price: 1.8,
    preemptiblePrice: 1.08,
  },
  {
    name: 'NVIDIA Tesla P4',
    type: 'nvidia-tesla-p4',
    numGpus: 1,
    maxNumCpus: 24,
    maxMem: 156,
    price: 0.6,
    preemptiblePrice: 0.216,
  },
  {
    name: 'NVIDIA Tesla P4',
    type: 'nvidia-tesla-p4',
    numGpus: 2,
    maxNumCpus: 48,
    maxMem: 312,
    price: 1.2,
    preemptiblePrice: 0.432,
  },
  {
    name: 'NVIDIA Tesla P4',
    type: 'nvidia-tesla-p4',
    numGpus: 4,
    maxNumCpus: 96,
    maxMem: 624,
    price: 1.8,
    preemptiblePrice: 0.864,
  },
  {
    name: 'NVIDIA Tesla V100',
    type: 'nvidia-tesla-v100',
    numGpus: 1,
    maxNumCpus: 12,
    maxMem: 78,
    price: 2.48,
    preemptiblePrice: 0.74,
  },
  {
    name: 'NVIDIA Tesla V100',
    type: 'nvidia-tesla-v100',
    numGpus: 2,
    maxNumCpus: 24,
    maxMem: 156,
    price: 4.96,
    preemptiblePrice: 1.48,
  },
  {
    name: 'NVIDIA Tesla V100',
    type: 'nvidia-tesla-v100',
    numGpus: 4,
    maxNumCpus: 48,
    maxMem: 312,
    price: 9.92,
    preemptiblePrice: 2.96,
  },
  {
    name: 'NVIDIA Tesla V100',
    type: 'nvidia-tesla-v100',
    numGpus: 8,
    maxNumCpus: 96,
    maxMem: 624,
    price: 19.84,
    preemptiblePrice: 5.92,
  },
];

export const gpuTypeToDisplayName = (type) => {
  return switchCase(
    type,
    ['nvidia-tesla-k80', () => 'NVIDIA Tesla K80'],
    ['nvidia-tesla-p4', () => 'NVIDIA Tesla P4'],
    ['nvidia-tesla-v100', () => 'NVIDIA Tesla V100'],
    [DEFAULT, () => 'NVIDIA Tesla T4']
  );
};

export const findGpu = (gpuType, numGpus) =>
  fp.find({ type: gpuType, numGpus: numGpus }, gpuTypes);

export const getValidGpuTypes = (numCpus, mem) =>
  fp.filter(
    ({ maxNumCpus, maxMem }) => numCpus <= maxNumCpus && mem <= maxMem,
    gpuTypes
  );

export const allMachineTypes: Machine[] = fp.map(
  ({ price, preemptiblePrice, ...details }) => ({
    price: price + 0.004,
    preemptiblePrice: preemptiblePrice + 0.002,
    ...details,
  }),
  machineBases
); // adding prices for ephemeral IP's, per https://cloud.google.com/compute/network-pricing#ipaddress

// Following constraints follow findings on RW-5763, last tested 11/19/20. Using
// machine types below this limit results in either an immediate ERROR status,
// or a delayed ERROR status after the runtime times out in the CREATING state.
// See also https://broadworkbench.atlassian.net/browse/SATURN-1337, where the
// determination was made for Dataproc master machines only.
export const validLeoGceMachineTypes = allMachineTypes.filter(
  ({ memory }) => memory >= 2
);
export const validLeoDataprocMasterMachineTypes = allMachineTypes.filter(
  ({ memory }) => memory > 13
);
// updated 29 Nov 2023 after observing the error:
// Creating clusters using the n1-standard-1 machine type is not supported for image version 2.1.11-debian11
export const validLeoDataprocWorkerMachineTypes = allMachineTypes.filter(
  ({ memory }) => memory >= 4
);

export const findMachineByName = (machineToFind: string) =>
  fp.find(({ name }) => name === machineToFind, allMachineTypes);

export const DEFAULT_MACHINE_NAME = 'n1-standard-4';
export const DEFAULT_MACHINE_TYPE: Machine =
  findMachineByName(DEFAULT_MACHINE_NAME);
export const DEFAULT_DISK_SIZE = MIN_DISK_SIZE_GB;

const approxHoursPerMonth = 730;
export const diskPricePerMonth = 0.04; // per GB month
export const diskPrice = diskPricePerMonth / approxHoursPerMonth; // per GB hour, from https://cloud.google.com/compute/pricing
export const ssdPricePerMonth = 0.17; // per GB month
export const dataprocCpuPrice = 0.01; // dataproc costs $0.01 per cpu per hour

const dataprocSurcharge = ({
  masterMachine,
  numberOfWorkers,
  numberOfPreemptibleWorkers,
  workerMachine,
}) => {
  const costs = [masterMachine.cpu * dataprocCpuPrice];
  if (workerMachine && numberOfWorkers) {
    costs.push(numberOfWorkers * workerMachine.cpu * dataprocCpuPrice);
  }
  if (workerMachine && numberOfPreemptibleWorkers) {
    costs.push(
      numberOfPreemptibleWorkers * workerMachine.cpu * dataprocCpuPrice
    );
  }
  return fp.sum(costs);
};

// The following calculations were based off of Terra UI's cost estimator:
// https://github.com/DataBiosphere/terra-ui/blob/cf5ec4408db3bd1fcdbcc5302da62d42e4d03ca3/src/components/ClusterManager.js#L85

export const diskConfigPricePerMonth = ({
  size,
  detachableType,
}: Partial<DiskConfig>) => {
  return (
    size *
    (detachableType === DiskType.SSD ? ssdPricePerMonth : diskPricePerMonth)
  );
};

export const detachableDiskPricePerMonth = (disk: Disk) => {
  return diskConfigPricePerMonth({
    size: disk.size,
    detachableType: disk.diskType,
  });
};

export const diskConfigPrice = (config: Partial<DiskConfig>) => {
  return diskConfigPricePerMonth(config) / approxHoursPerMonth;
};

const detachableDiskPrice = (disk: Disk) => {
  return detachableDiskPricePerMonth(disk) / approxHoursPerMonth;
};

export const machineStorageCost = ({
  diskConfig,
  dataprocConfig,
  detachedDisk,
}: AnalysisConfig) => {
  const { numberOfWorkers, numberOfPreemptibleWorkers, workerDiskSize } =
    dataprocConfig ?? {};
  return fp.sum([
    diskConfigPrice(diskConfig),
    detachedDisk ? detachableDiskPrice(detachedDisk) : 0,
    numberOfWorkers ? numberOfWorkers * workerDiskSize * diskPrice : 0,
    numberOfPreemptibleWorkers
      ? numberOfPreemptibleWorkers * workerDiskSize * diskPrice
      : 0,
  ]);
};

export const machineStorageCostBreakdown = ({
  diskConfig,
  dataprocConfig,
  detachedDisk,
}: AnalysisConfig) => {
  const { numberOfWorkers, numberOfPreemptibleWorkers, workerDiskSize } =
    dataprocConfig ?? {};
  const costs = [];
  if (dataprocConfig) {
    costs.push(`${formatUsd(diskConfigPrice(diskConfig))}/hr Master Disk`);
    if (numberOfWorkers) {
      costs.push(
        `${formatUsd(
          numberOfWorkers * workerDiskSize * diskPrice
        )}/hr Worker Disk(s)`
      );
    }
    if (numberOfPreemptibleWorkers) {
      costs.push(
        `${formatUsd(
          numberOfPreemptibleWorkers * workerDiskSize * diskPrice
        )}/hr Preemptible Worker Disk(s)`
      );
    }
  } else {
    costs.push(`${formatUsd(diskConfigPrice(diskConfig))}/hr Disk`);
  }
  if (detachedDisk) {
    costs.push(
      `${formatUsd(detachableDiskPrice(detachedDisk))}/hr Detached Disk`
    );
  }
  return costs;
};

export const machineRunningCost = (analysisConfig: AnalysisConfig) => {
  const { computeType, machine, gpuConfig, numNodes } = analysisConfig;
  const { workerMachineType, numberOfWorkers, numberOfPreemptibleWorkers } =
    analysisConfig.dataprocConfig ?? {};

  const workerMachine =
    workerMachineType && findMachineByName(workerMachineType);
  const gpu = gpuConfig && findGpu(gpuConfig.gpuType, gpuConfig.numOfGpus);
  const dataprocPrice =
    computeType === ComputeType.Dataproc
      ? fp.sum([
          dataprocSurcharge({
            masterMachine: machine,
            numberOfWorkers,
            numberOfPreemptibleWorkers,
            workerMachine,
          }),
          workerMachine
            ? fp.sum([
                numberOfWorkers * workerMachine.price,
                numberOfPreemptibleWorkers * workerMachine.preemptiblePrice,
              ])
            : 0,
        ])
      : 0;
  return fp.sum([
    dataprocPrice,
    machine.price * (numNodes ?? 1),
    gpu ? gpu.price : 0,
    machineStorageCost(analysisConfig),
  ]);
};

export const machineRunningCostBreakdown = (analysisConfig: AnalysisConfig) => {
  const { computeType, machine, gpuConfig } = analysisConfig;
  const { workerMachineType, numberOfWorkers, numberOfPreemptibleWorkers } =
    analysisConfig.dataprocConfig ?? {};

  const workerMachine =
    workerMachineType && findMachineByName(workerMachineType);
  const gpu = gpuConfig && findGpu(gpuConfig.gpuType, gpuConfig.numOfGpus);
  const costs = [];
  if (computeType === ComputeType.Dataproc) {
    if (workerMachine) {
      costs.push(`${formatUsd(machine.price)}/hr Master VM`);
      if (numberOfWorkers > 0) {
        costs.push(
          `${formatUsd(
            workerMachine.price * numberOfWorkers
          )}/hr Worker VM(s) (${numberOfWorkers})`
        );
      }
      if (numberOfPreemptibleWorkers > 0) {
        costs.push(
          `${formatUsd(
            workerMachine.preemptiblePrice * numberOfPreemptibleWorkers
          )}/hr Preemptible Worker VM(s) ` + `(${numberOfPreemptibleWorkers})`
        );
      }
    }
    const dataprocSurchargeAmount = dataprocSurcharge({
      masterMachine: machine,
      numberOfWorkers: numberOfWorkers,
      numberOfPreemptibleWorkers: numberOfPreemptibleWorkers,
      workerMachine: workerMachine,
    });
    costs.push(
      `${formatUsd(dataprocSurchargeAmount)}/hr Dataproc Per-CPU Surcharge`
    );
  } else {
    costs.push(`${formatUsd(machine.price)}/hr VM`);
    if (gpu) {
      costs.push(`${formatUsd(gpu.price)}/hr GPU`);
    }
  }
  costs.push(...machineStorageCostBreakdown(analysisConfig));
  return costs;
};
