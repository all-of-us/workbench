import { Disk, Runtime, RuntimeStatus } from 'generated/fetch';

import { applyPresetOverride } from 'app/utils/runtime-presets';
import {
  AnalysisConfig,
  maybeWithPersistentDisk,
  toAnalysisConfig,
} from 'app/utils/runtime-utils';

interface InitDerivedInProps {
  crFromCustomRuntimeHook: Runtime;
  pendingRuntime: Runtime;
  gcePersistentDisk: Disk;
}

interface InitDerivedOutProps {
  currentRuntime: Runtime;
  existingAnalysisConfig: AnalysisConfig;
}

export const initDerivedValues = ({
  crFromCustomRuntimeHook,
  pendingRuntime,
  gcePersistentDisk,
}: InitDerivedInProps): InitDerivedOutProps => {
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
