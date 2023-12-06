import validate from 'validate.js';

import { Disk, Runtime, RuntimeStatus } from 'generated/fetch';

import { summarizeErrors } from 'app/utils';
import {
  AnalysisConfig,
  maybeWithPersistentDisk,
  toAnalysisConfig,
} from 'app/utils/analysis-config';
import {
  ComputeType,
  DATAPROC_MIN_DISK_SIZE_GB,
  machineRunningCost,
  MIN_DISK_SIZE_GB,
} from 'app/utils/machines';
import { applyPresetOverride } from 'app/utils/runtime-presets';
import { serverConfigStore } from 'app/utils/stores';

interface DeriveConfigProps {
  crFromCustomRuntimeHook: Runtime;
  pendingRuntime: Runtime;
  gcePersistentDisk: Disk;
}
interface DerivedConfigResult {
  currentRuntime: Runtime;
  existingAnalysisConfig: AnalysisConfig;
}
export const deriveConfiguration = ({
  crFromCustomRuntimeHook,
  pendingRuntime,
  gcePersistentDisk,
}: DeriveConfigProps): DerivedConfigResult => {
  // If the runtime has been deleted, it's possible that the default preset values have changed since its creation
  const currentRuntime =
    crFromCustomRuntimeHook &&
    crFromCustomRuntimeHook.status === RuntimeStatus.DELETED
      ? applyPresetOverride(
          // The attached disk information is lost for deleted runtimes. In any case,
          // by default we want to offer that the user reattach their existing disk,
          // if any and if the configuration allows it.
          maybeWithPersistentDisk(crFromCustomRuntimeHook, gcePersistentDisk)
        )
      : crFromCustomRuntimeHook;

  // Prioritize the "pendingRuntime", if any. When an update is pending, we want
  // to render the target runtime details, which  may not match the current runtime.
  const existingRuntime =
    pendingRuntime || currentRuntime || ({} as Partial<Runtime>);
  const existingAnalysisConfig = toAnalysisConfig(
    existingRuntime,
    gcePersistentDisk
  );

  return { currentRuntime, existingAnalysisConfig };
};

interface DeriveErrorsWarningsProps {
  usingInitialCredits: boolean;
  creatorFreeCreditsRemaining?: number;
  analysisConfig: AnalysisConfig;
}
interface DeriveErrorsWarningsResult {
  getErrorMessageContent: () => JSX.Element[];
  getWarningMessageContent: () => JSX.Element[];
}
export const deriveErrorsAndWarnings = ({
  usingInitialCredits,
  creatorFreeCreditsRemaining = 0,
  analysisConfig,
}: DeriveErrorsWarningsProps): DeriveErrorsWarningsResult => {
  // Leonardo enforces a minimum limit for disk size, 4000 GB is our arbitrary limit for not making a
  // disk that is way too big and expensive on free tier ($.22 an hour). 64 TB is the GCE limit on
  // persistent disk.
  const diskSizeValidatorWithMessage = (
    diskType = 'standard' || 'master' || 'worker'
  ) => {
    const maxDiskSize = usingInitialCredits ? 4000 : 64000;
    const minDiskSize =
      analysisConfig.computeType === ComputeType.Dataproc
        ? DATAPROC_MIN_DISK_SIZE_GB
        : MIN_DISK_SIZE_GB;
    const message = {
      standard: `^Disk size must be between ${minDiskSize} and ${maxDiskSize} GB`,
      master: `^Master disk size must be between ${DATAPROC_MIN_DISK_SIZE_GB} and ${maxDiskSize} GB`,
      worker: `^Worker disk size must be between ${DATAPROC_MIN_DISK_SIZE_GB} and ${maxDiskSize} GB`,
    };

    return {
      numericality: {
        greaterThanOrEqualTo:
          analysisConfig.computeType === ComputeType.Dataproc
            ? DATAPROC_MIN_DISK_SIZE_GB
            : MIN_DISK_SIZE_GB,
        lessThanOrEqualTo: maxDiskSize,
        message: message[diskType],
      },
    };
  };

  const costErrorsAsWarnings =
    !usingInitialCredits ||
    // We've increased the workspace creator's free credits. This means they may be expecting to run
    // a more expensive analysis, and the program has extended some further trust for free credit
    // use. Allow them to provision a larger runtime (still warn them). Block them if they get below
    // the default amount of free credits because (1) this can result in overspend and (2) we have
    // easy access to remaining credits, and not the creator's quota.
    creatorFreeCreditsRemaining >
      serverConfigStore.get().config.defaultFreeCreditsDollarLimit;

  const runningCostValidatorWithMessage = () => {
    const maxRunningCost = usingInitialCredits ? 25 : 150;
    const message = costErrorsAsWarnings
      ? '^Your runtime is expensive. Are you sure you wish to proceed?'
      : `^Your runtime is too expensive. To proceed using free credits, reduce your running costs below $${maxRunningCost}/hr.`;
    return {
      numericality: {
        lessThan: maxRunningCost,
        message: message,
      },
    };
  };

  const runningCostErrors = validate(
    { currentRunningCost: machineRunningCost(analysisConfig) },
    {
      currentRunningCost: runningCostValidatorWithMessage(),
    }
  );

  const getErrorMessageContent = (): JSX.Element[] => {
    const diskErrors = validate(
      { diskSize: analysisConfig.diskConfig.size },
      {
        diskSize: diskSizeValidatorWithMessage('standard'),
      }
    );

    const { masterDiskSize, workerDiskSize, numberOfWorkers } =
      analysisConfig.dataprocConfig || {};
    const dataprocErrors =
      analysisConfig.computeType === ComputeType.Dataproc &&
      validate(
        { masterDiskSize, workerDiskSize, numberOfWorkers },
        // We don't clear dataproc config when we change compute type so we can't combine this with the
        // runningCostValidator or else we can end up with phantom validation fails
        {
          masterDiskSize: diskSizeValidatorWithMessage('master'),
          workerDiskSize: diskSizeValidatorWithMessage('worker'),
          numberOfWorkers: {
            numericality: {
              greaterThanOrEqualTo: 2,
              message: 'Dataproc requires at least 2 worker nodes',
            },
          },
        }
      );

    return [
      ...(diskErrors ? summarizeErrors(diskErrors) : []),
      ...(dataprocErrors ? summarizeErrors(dataprocErrors) : []),
      // only report cost errors -as errors- if costErrorsAsWarnings is false
      ...(!costErrorsAsWarnings && runningCostErrors
        ? summarizeErrors(runningCostErrors)
        : []),
    ];
  };

  const getWarningMessageContent = (): JSX.Element[] =>
    // if costErrorsAsWarnings is false, we report them as errors.  See getErrorMessageContent()
    costErrorsAsWarnings && runningCostErrors
      ? summarizeErrors(runningCostErrors)
      : [];

  return {
    getErrorMessageContent,
    getWarningMessageContent,
  };
};
