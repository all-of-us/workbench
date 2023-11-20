import {
  Disk,
  Runtime,
  RuntimeConfigurationType,
  RuntimeStatus,
} from 'generated/fetch';

import { cond } from '@terra-ui-packages/core-utils';
import { applyPresetOverride } from 'app/utils/runtime-presets';
import {
  AnalysisConfig,
  maybeWithPersistentDisk,
  PanelContent,
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

interface InitPanelProps {
  initialPanelContent: PanelContent;
  pendingRuntime: Runtime;
  currentRuntime: Runtime;
  runtimeStatus: RuntimeStatus;
}
export const initializePanelContent = ({
  initialPanelContent,
  pendingRuntime,
  currentRuntime,
  runtimeStatus,
}: InitPanelProps): PanelContent =>
  cond<PanelContent>(
    [!!initialPanelContent, () => initialPanelContent],
    // If there's a pendingRuntime, this means there's already a create/update
    // in progress, even if the runtime store doesn't actively reflect this yet.
    // Show the customize panel in this event.
    [!!pendingRuntime, () => PanelContent.Customize],
    [
      currentRuntime === null ||
        currentRuntime === undefined ||
        runtimeStatus === RuntimeStatus.UNKNOWN,
      () => PanelContent.Create,
    ],
    [
      // General Analysis consist of GCE + PD. Display create page only if
      // 1) currentRuntime + pd both are deleted and
      // 2) configurationType is either GeneralAnalysis or HailGenomicAnalysis
      currentRuntime?.status === RuntimeStatus.DELETED &&
        !currentRuntime?.gceWithPdConfig &&
        (
          [
            RuntimeConfigurationType.GENERAL_ANALYSIS,
            RuntimeConfigurationType.HAIL_GENOMIC_ANALYSIS,
          ] as Array<RuntimeConfigurationType>
        ).includes(currentRuntime?.configurationType),
      () => PanelContent.Create,
    ],
    () => PanelContent.Customize
  );
