import * as React from 'react';

import {
  DiskType,
  ErrorCode,
  ListRuntimeResponse,
  Runtime,
  RuntimeError,
  RuntimeStatus,
  SecuritySuspendedErrorParameters,
} from 'generated/fetch';

import {
  ExceededErrorCountError,
  LeoRuntimeInitializer,
} from 'app/utils/leo-runtime-initializer';
import {
  compoundRuntimeOpStore,
  userAppsStore,
  useStore,
} from 'app/utils/stores';

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
