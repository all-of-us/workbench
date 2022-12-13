import * as React from 'react';

import { RuntimeStatus } from 'generated/fetch';

import { switchCase } from 'app/utils';
import { machineRunningCost, machineStorageCost } from 'app/utils/machines';
import { formatUsd } from 'app/utils/numbers';
import { isVisible, toAnalysisConfig } from 'app/utils/runtime-utils';
import { diskStore, runtimeStore, useStore } from 'app/utils/stores';

export const RuntimeCost = () => {
  const { runtime } = useStore(runtimeStore);
  const { persistentDisk } = useStore(diskStore);

  if (!isVisible(runtime?.status)) {
    return null;
  }

  const analysisConfig = toAnalysisConfig(runtime, persistentDisk);
  const runningCost = formatUsd(machineRunningCost(analysisConfig));
  const storageCost = formatUsd(machineStorageCost(analysisConfig));

  // display running cost or stopped (storage) cost
  // Error and Deleted statuses are not included because they're not "visible" [isVisible() = false]
  const text: string = switchCase(
    runtime.status,
    // TODO: is it appropriate to assume full running cost in all these cases?
    [RuntimeStatus.Creating, () => `${runtime.status} ${runningCost} / hr`],
    [RuntimeStatus.Running, () => `${runtime.status} ${runningCost} / hr`],
    [RuntimeStatus.Updating, () => `${runtime.status} ${runningCost} / hr`],
    [RuntimeStatus.Deleting, () => `${runtime.status} ${runningCost} / hr`],
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
