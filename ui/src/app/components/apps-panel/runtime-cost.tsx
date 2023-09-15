import * as React from 'react';

import { RuntimeStatus } from 'generated/fetch';

import { switchCase } from '@terra-ui-packages/core-utils';
import { machineRunningCost, machineStorageCost } from 'app/utils/machines';
import { formatUsd } from 'app/utils/numbers';
import { isVisible, toAnalysisConfig } from 'app/utils/runtime-utils';
import { runtimeDiskStore, runtimeStore, useStore } from 'app/utils/stores';

export const RuntimeCost = () => {
  const { runtime } = useStore(runtimeStore);
  const { gcePersistentDisk } = useStore(runtimeDiskStore);

  if (!isVisible(runtime?.status)) {
    return null;
  }

  const analysisConfig = toAnalysisConfig(runtime, gcePersistentDisk);
  const runningCost = formatUsd(machineRunningCost(analysisConfig));
  const storageCost = formatUsd(machineStorageCost(analysisConfig));

  // display running cost or stopped (storage) cost
  // Error and Deleted statuses are not included because they're not "visible" [isVisible() = false]
  const text: RuntimeStatus | string = switchCase<
    RuntimeStatus,
    RuntimeStatus | string
  >(
    runtime.status,
    // TODO: is it appropriate to assume full running cost in all these cases?
    [RuntimeStatus.CREATING, () => `${runtime.status} ${runningCost} / hr`],
    [RuntimeStatus.RUNNING, () => `${runtime.status} ${runningCost} / hr`],
    [RuntimeStatus.Updating, () => `${runtime.status} ${runningCost} / hr`],
    [RuntimeStatus.DELETING, () => `${runtime.status} ${runningCost} / hr`],
    [RuntimeStatus.Stopping, () => `Pausing ${runningCost} / hr`],
    [RuntimeStatus.Starting, () => `Resuming ${runningCost} / hr`],
    [RuntimeStatus.Stopped, () => `Paused ${storageCost} / hr`],
    [RuntimeStatus.Unknown, () => runtime.status]
  );

  return (
    <div data-test-id='runtime-cost' style={{ alignSelf: 'center' }}>
      {text}
    </div>
  );
};
