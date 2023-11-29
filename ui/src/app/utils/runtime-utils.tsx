import * as React from 'react';
import * as fp from 'lodash/fp';

import {
  DataprocConfig,
  Disk,
  DiskType,
  ErrorCode,
  GpuConfig,
  ListRuntimeResponse,
  PersistentDiskRequest,
  Runtime,
  RuntimeConfigurationType,
  RuntimeError,
  RuntimeStatus,
  SecuritySuspendedErrorParameters,
} from 'generated/fetch';

import { cond } from '@terra-ui-packages/core-utils';
import {
  ExceededErrorCountError,
  LeoRuntimeInitializer,
} from 'app/utils/leo-runtime-initializer';
import {
  ComputeType,
  DATAPROC_MIN_DISK_SIZE_GB,
  DEFAULT_AUTOPAUSE_THRESHOLD_MINUTES,
  DEFAULT_DISK_SIZE,
  DEFAULT_MACHINE_TYPE,
  findMachineByName,
  Machine,
} from 'app/utils/machines';
import {
  compoundRuntimeOpStore,
  userAppsStore,
  useStore,
} from 'app/utils/stores';

import { runtimePresets } from './runtime-presets';

export class ComputeSecuritySuspendedError extends Error {
  constructor(public params: SecuritySuspendedErrorParameters) {
    super('user is suspended from compute');
    // See https://github.com/Microsoft/TypeScript/wiki/Breaking-Changes#extending-built-ins-like-error-array-and-map-may-no-longer-work
    Object.setPrototypeOf(this, ComputeSecuritySuspendedError.prototype);

    this.name = 'ComputeSecuritySuspendedError';
  }
}

export class RuntimeStatusError extends Error {
  constructor(public errors?: RuntimeError[]) {
    super(
      'runtime creation failed:\n' +
        errors?.map((m) => m.errorMessage).join('\n')
    );
    // See https://github.com/Microsoft/TypeScript/wiki/Breaking-Changes#extending-built-ins-like-error-array-and-map-may-no-longer-work
    Object.setPrototypeOf(this, RuntimeStatusError.prototype);

    this.name = 'RuntimeStatusError';
  }
}

export enum RuntimeStatusRequest {
  DeleteRuntime = 'DeleteRuntime',
  DeleteRuntimeAndPD = 'DeleteRuntimeAndPD',
  DeletePD = 'DeletePD',
  Start = 'Start',
  Stop = 'Stop',
}

export const diskTypeLabels = {
  [DiskType.STANDARD]: 'Standard Disk',
  [DiskType.SSD]: 'Solid State Disk',
};

export interface DiskConfig {
  size: number;
  detachable: boolean;
  detachableType: DiskType | null;
  existingDiskName: string | null;
}

export interface AnalysisConfig {
  computeType: ComputeType;
  machine: Machine;
  diskConfig: DiskConfig;
  // TODO: Document types of disks available (RW-9490)
  // This should only be populated if !diskconfig.detachable.
  detachedDisk: Disk;
  // TODO: Refactor this type to an intermediate representation.
  dataprocConfig: DataprocConfig;
  // TODO: Refactor this type to an intermediate representation.
  gpuConfig: GpuConfig;
  autopauseThreshold: number;
  numNodes?: number;
}

export interface UpdateMessaging {
  applyAction: string;
  warn?: string;
  warnMore?: string;
}

export const RUNTIME_ERROR_STATUS_MESSAGE_SHORT =
  'An error was encountered with your cloud environment. ' +
  'To resolve, please see the cloud analysis environment side panel.';

const errorToSecuritySuspendedParams = async (
  error
): Promise<SecuritySuspendedErrorParameters> => {
  if (error?.status !== 412) {
    return null;
  }
  const body = await error?.json();
  if (body?.errorCode !== ErrorCode.COMPUTE_SECURITY_SUSPENDED) {
    return null;
  }

  return body?.parameters as SecuritySuspendedErrorParameters;
};

export const maybeUnwrapSecuritySuspendedError = async (
  error: Error
): Promise<Error> => {
  if (error instanceof ExceededErrorCountError) {
    error = error.lastError;
  }
  const suspendedParams = await errorToSecuritySuspendedParams(error);
  if (suspendedParams) {
    return new ComputeSecuritySuspendedError(suspendedParams);
  }
  return error;
};

// Returns true if two runtimes are equivalent in terms of the fields which are
// affected by runtime presets.
const presetEquals = (a: Runtime, b: Runtime): boolean => {
  const strip = fp.flow(
    // In the future, things like toolDockerImage and autopause may be considerations.
    // With https://precisionmedicineinitiative.atlassian.net/browse/RW-9167, general analysis
    // should have persistent disk
    fp.pick(['gceWithPdConfig', 'dataprocConfig']),
    // numberOfWorkerLocalSSDs is currently part of the API spec, but is not used by the panel.
    fp.omit(['dataprocConfig.numberOfWorkerLocalSSDs'])
  );
  return fp.isEqual(strip(a), strip(b));
};

export const fromAnalysisConfig = (analysisConfig: AnalysisConfig): Runtime => {
  const {
    autopauseThreshold,
    computeType,
    dataprocConfig,
    diskConfig,
    gpuConfig,
    machine: { name: machineType },
  } = analysisConfig;

  const runtime: Runtime = {
    autopauseThreshold,
  };
  if (computeType === ComputeType.Dataproc) {
    runtime.dataprocConfig = {
      ...dataprocConfig,
      masterMachineType: machineType,
      masterDiskSize: diskConfig.size,
    };
  } else if (diskConfig.detachable) {
    runtime.gceWithPdConfig = {
      machineType,
      gpuConfig,
      persistentDisk: {
        size: diskConfig.size,
        diskType: diskConfig.detachableType,
        labels: {},
        name: diskConfig.existingDiskName,
      },
    };
  } else {
    runtime.gceConfig = {
      machineType,
      gpuConfig,
      diskSize: diskConfig.size,
    };
  }

  // If the selected runtime matches a preset, plumb through the appropriate configuration type.
  runtime.configurationType =
    fp.get(
      'runtimeTemplate.configurationType',
      fp.find(
        ({ runtimeTemplate }) => presetEquals(runtime, runtimeTemplate),
        runtimePresets
      )
    ) || RuntimeConfigurationType.USER_OVERRIDE;

  return runtime;
};

export const canUseExistingDisk = (
  { detachableType, size }: Partial<DiskConfig>,
  existingDisk: Disk | null
) => {
  return (
    !!existingDisk &&
    detachableType === existingDisk.diskType &&
    size >= existingDisk.size
  );
};

export const maybeWithExistingDiskName = (
  c: Omit<DiskConfig, 'existingDiskName'>,
  existingDisk: Disk | null
): DiskConfig => {
  if (canUseExistingDisk(c, existingDisk)) {
    return { ...c, existingDiskName: existingDisk.name };
  }
  return { ...c, existingDiskName: null };
};

export const maybeWithPersistentDisk = (
  runtime: Runtime,
  persistentDisk: Disk | PersistentDiskRequest | null | undefined
): Runtime => {
  if (!runtime || !persistentDisk || !runtime.gceConfig) {
    return runtime;
  }
  // TODO: why not all fields?
  const { name, size, diskType } = persistentDisk;
  return {
    ...runtime,
    gceConfig: null, // TODO: why not undefined?
    gceWithPdConfig: {
      ...runtime.gceConfig, // note: gceConfig.diskSize is discarded.  this is what we want.
      persistentDisk: { name, size, diskType },
    },
  };
};

// TODO - this is way more complex than it needs to be, and likely has some errors
export const withAnalysisConfigDefaults = (
  r: AnalysisConfig,
  existingPersistentDisk: Disk | null
): AnalysisConfig => {
  let {
    diskConfig: { size, detachable, detachableType },
    gpuConfig,
    dataprocConfig,
  } = r;
  let existingDiskName = null;
  const computeType = r.computeType ?? ComputeType.Standard;
  // For computeType Standard: We are moving away from storage disk as Standard
  // As part of RW-9167, we are disabling Standard storage disk if computeType is standard
  // Eventually we will be removing this option altogether
  if (computeType === ComputeType.Standard) {
    if (existingPersistentDisk) {
      detachable = true;
      size = size ?? existingPersistentDisk?.size ?? DEFAULT_DISK_SIZE;
      detachableType =
        detachableType ?? existingPersistentDisk?.diskType ?? DiskType.STANDARD;
      if (canUseExistingDisk(r.diskConfig, existingPersistentDisk)) {
        existingDiskName = existingPersistentDisk.name;
      }
    } else {
      // No existing disk.
      detachableType = DiskType.STANDARD;
      detachable = true;
    }
  } else if (computeType === ComputeType.Dataproc) {
    detachable = false;
    detachableType = null;
    gpuConfig = null;

    const defaults = runtimePresets.hailAnalysis.runtimeTemplate.dataprocConfig;
    dataprocConfig = {
      numberOfWorkers:
        dataprocConfig?.numberOfWorkers ?? defaults.numberOfWorkers,
      workerMachineType:
        dataprocConfig?.workerMachineType ?? defaults.workerMachineType,
      workerDiskSize: dataprocConfig?.workerDiskSize ?? defaults.workerDiskSize,
      numberOfPreemptibleWorkers:
        dataprocConfig?.numberOfPreemptibleWorkers ??
        defaults.numberOfPreemptibleWorkers,
    };
    size = size ?? existingPersistentDisk?.size ?? DATAPROC_MIN_DISK_SIZE_GB;
  } else {
    throw Error(`unknown computeType: '${computeType}'`);
  }

  return {
    computeType,
    machine: r.machine ?? DEFAULT_MACHINE_TYPE,
    diskConfig: {
      size: size ?? DEFAULT_DISK_SIZE,
      detachable,
      detachableType,
      existingDiskName,
    },
    detachedDisk: detachable ? null : existingPersistentDisk,
    dataprocConfig,
    gpuConfig,
    autopauseThreshold:
      r.autopauseThreshold ?? DEFAULT_AUTOPAUSE_THRESHOLD_MINUTES,
  };
};

export const toAnalysisConfig = (
  runtime: Runtime,
  existingDisk: Disk | null
): AnalysisConfig => {
  const toGceConfig = () => {
    const { machineType, diskSize, gpuConfig } = runtime.gceConfig;
    return {
      computeType: ComputeType.Standard,
      machine: findMachineByName(machineType),
      diskConfig: {
        size: diskSize,
        detachable: false,
        detachableType: null,
        existingDiskName: null,
      },
      detachedDisk: existingDisk,
      autopauseThreshold: runtime.autopauseThreshold,
      dataprocConfig: null,
      gpuConfig,
    };
  };
  const toGceWithPdConfig = () => {
    const {
      machineType,
      persistentDisk: { size, diskType: detachableType },
      gpuConfig,
    } = runtime.gceWithPdConfig;
    return {
      computeType: ComputeType.Standard,
      machine: findMachineByName(machineType),
      diskConfig: maybeWithExistingDiskName(
        {
          size,
          detachable: true,
          detachableType,
        },
        existingDisk
      ),
      detachedDisk: null,
      autopauseThreshold: runtime.autopauseThreshold,
      dataprocConfig: null,
      gpuConfig,
    };
  };
  const toDataprocConfig = () => {
    const { dataprocConfig, autopauseThreshold } = runtime;
    const { masterMachineType, masterDiskSize } = dataprocConfig;
    return {
      computeType: ComputeType.Dataproc,
      machine: findMachineByName(masterMachineType),
      diskConfig: {
        size: masterDiskSize,
        detachable: false,
        detachableType: null,
        existingDiskName: null,
      },
      detachedDisk: existingDisk,
      autopauseThreshold,
      dataprocConfig,
      gpuConfig: null,
    };
  };
  const toEmptyConfig = () => ({
    computeType: null,
    machine: null,
    diskConfig: {
      size: null,
      detachable: null,
      detachableType: null,
      existingDiskName: null,
    },
    detachedDisk: existingDisk,
    autopauseThreshold: null,
    dataprocConfig: null,
    gpuConfig: null,
  });

  return cond(
    [!!runtime.gceConfig, toGceConfig],
    [!!runtime.gceWithPdConfig, toGceWithPdConfig],
    [!!runtime.dataprocConfig, toDataprocConfig],
    toEmptyConfig
  );
};

export const maybeInitializeRuntime = async (
  workspaceNamespace: string,
  signal: AbortSignal,
  targetRuntime?: Runtime
): Promise<Runtime> => {
  if (workspaceNamespace in compoundRuntimeOpStore.get()) {
    await new Promise<void>((resolve, reject) => {
      signal.addEventListener('abort', reject);
      const { unsubscribe } = compoundRuntimeOpStore.subscribe((v) => {
        if (!(workspaceNamespace in v)) {
          unsubscribe();
          signal.removeEventListener('abort', reject);
          resolve();
        }
      });
    });
  }

  try {
    const runtime = await LeoRuntimeInitializer.initialize({
      workspaceNamespace,
      pollAbortSignal: signal,
      targetRuntime,
    });
    if (runtime.status === RuntimeStatus.ERROR) {
      throw new RuntimeStatusError(runtime.errors);
    }
    return runtime;
  } catch (error) {
    throw await maybeUnwrapSecuritySuspendedError(error);
  }
};

export const withUserAppsStore = () => (WrappedComponent) => {
  return (props) => {
    const value = useStore(userAppsStore);

    return <WrappedComponent {...props} userAppsStore={value} />;
  };
};

export enum SparkConsolePath {
  Yarn = 'yarn',
  YarnTimeline = 'apphistory',
  SparkHistory = 'sparkhistory',
  JobHistory = 'jobhistory',
}

export enum PanelContent {
  Create = 'Create',
  Customize = 'Customize',
  DeleteRuntime = 'DeleteRuntime',
  DeleteUnattachedPd = 'DeleteUnattachedPd',
  DeleteUnattachedPdAndCreate = 'DeleteUnattachedPdAndCreate',
  Disabled = 'Disabled',
  ConfirmUpdate = 'ConfirmUpdate',
  ConfirmUpdateWithDiskDelete = 'ConfirmUpdateWithDiskDelete',
  SparkConsole = 'SparkConsole',
}

// should we show the runtime in the UI (in most situations)?
// Note: we do make users aware of ERROR runtimes in some situations
export const isVisible = (status: RuntimeStatus) =>
  status &&
  !(
    [RuntimeStatus.DELETED, RuntimeStatus.ERROR] as Array<RuntimeStatus>
  ).includes(status);

// can the user delete the runtime?
export const canDeleteRuntime = (status: RuntimeStatus) =>
  (
    [
      RuntimeStatus.RUNNING,
      RuntimeStatus.STOPPED,
      RuntimeStatus.ERROR,
    ] as Array<RuntimeStatus>
  ).includes(status);

export const getCreator = (runtime: ListRuntimeResponse): string | undefined =>
  // eslint-disable-next-line @typescript-eslint/dot-notation
  runtime?.labels?.['creator'];
