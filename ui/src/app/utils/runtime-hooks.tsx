import * as React from 'react';
import * as fp from 'lodash/fp';

import {
  Disk,
  EmptyResponse,
  PersistentDiskRequest,
  Runtime,
  RuntimeStatus,
} from 'generated/fetch';

import { switchCase } from '@terra-ui-packages/core-utils';
import { leoRuntimesApi } from 'app/services/notebooks-swagger-fetch-clients';
import { disksApi, runtimeApi } from 'app/services/swagger-fetch-clients';

import { canUseExistingDisk, toAnalysisConfig } from './analysis-config';
import { withAsyncErrorHandling } from './index';
import {
  ExceededActionCountError,
  LeoRuntimeInitializationAbortedError,
  LeoRuntimeInitializer,
} from './leo-runtime-initializer';
import {
  AnalysisDiffState,
  findMostSevereDiffState,
  getAnalysisConfigDiffs,
} from './runtime-diffs';
import {
  isVisible,
  maybeUnwrapSecuritySuspendedError,
  RuntimeStatusRequest,
} from './runtime-utils';
import {
  compoundRuntimeOpStore,
  markCompoundRuntimeOperationCompleted,
  registerCompoundRuntimeOperation,
  runtimeDiskStore,
  runtimeStore,
  useStore,
} from './stores';

const { useState, useEffect } = React;

const diskNeedsSizeIncrease = (
  requestedDisk: PersistentDiskRequest | null,
  existingDisk: Disk | null
) => {
  if (!requestedDisk) {
    return false;
  }
  const { diskType, size } = requestedDisk;
  return (
    canUseExistingDisk({ detachableType: diskType, size }, existingDisk) &&
    size > existingDisk.size
  );
};

export const useRuntime = (currentWorkspaceNamespace) => {
  // No cleanup is being handled at the moment.
  // When the user initiates a runtime change we want that change to take place even if they navigate away
  useEffect(() => {
    if (!currentWorkspaceNamespace) {
      return;
    }

    const getRuntime = withAsyncErrorHandling(
      () =>
        runtimeStore.set({
          workspaceNamespace: undefined,
          runtime: undefined,
          runtimeLoaded: false,
        }),
      async () => {
        let leoRuntime: Runtime;
        try {
          leoRuntime = await runtimeApi().getRuntime(currentWorkspaceNamespace);
        } catch (e) {
          if (e instanceof Response && e.status === 404) {
            // null on the runtime store indicates no existing runtime
            leoRuntime = null;
          } else {
            runtimeStore.set({
              workspaceNamespace: undefined,
              runtime: undefined,
              runtimeLoaded: false,
              loadingError: await maybeUnwrapSecuritySuspendedError(e),
            });
            return;
          }
        }
        const currentStore = runtimeStore.get();
        if (currentWorkspaceNamespace === currentStore.workspaceNamespace) {
          const newStore = {
            ...currentStore,
            runtime: leoRuntime,
            runtimeLoaded: true,
          };
          // checking for (deep) value equality substantially reduces the number of runtimeStore updates over the
          // default (reference) equality check, because runtime is often a new object
          if (!fp.isEqual(currentStore, newStore)) {
            runtimeStore.set(newStore);
          }
        }
      }
    );
    getRuntime();
  }, [currentWorkspaceNamespace]);
};

// useDisk hook is a simple hook to populate the disk store.
// This is only used by other disk hooks
export const useDisk = (currentWorkspaceNamespace: string) => {
  useEffect(() => {
    if (!currentWorkspaceNamespace) {
      return;
    }
    const getDisk = withAsyncErrorHandling(
      () =>
        runtimeDiskStore.set({
          workspaceNamespace: null,
          gcePersistentDisk: null,
        }),
      async () => {
        let gcePersistentDisk: Disk = null;
        try {
          const availableDisks = await disksApi().listOwnedDisksInWorkspace(
            currentWorkspaceNamespace
          );
          gcePersistentDisk = availableDisks.find((disk) => !!disk.gceRuntime);
        } catch (e) {
          if (!(e instanceof Response && e.status === 404)) {
            throw e;
          }
        }
        if (
          currentWorkspaceNamespace ===
          runtimeDiskStore.get().workspaceNamespace
        ) {
          runtimeDiskStore.set({
            workspaceNamespace: currentWorkspaceNamespace,
            gcePersistentDisk,
          });
        }
      }
    );
    getDisk();
  }, [currentWorkspaceNamespace]);
};

// useRuntimeStatus hook can be used to change the status of the runtime
// This setter returns a promise which resolves when any proximal fetch has completed,
// but does not wait for any polling, which may continue asynchronously.
export const useRuntimeStatus = (
  currentWorkspaceNamespace,
  currentGoogleProject
): [
  RuntimeStatus | undefined,
  (statusRequest: RuntimeStatusRequest) => Promise<void>
] => {
  const [runtimeStatusRequest, setRuntimeStatusRequest] =
    useState<RuntimeStatusRequest>();
  const { runtime } = useStore(runtimeStore);
  // Ensure that a runtime gets initialized, if it hasn't already been.
  useRuntime(currentWorkspaceNamespace);
  useDisk(currentWorkspaceNamespace);
  useEffect(() => {
    // Additional status changes can be put here
    const resolutionCondition: (r: Runtime) => boolean = switchCase(
      runtimeStatusRequest,
      [
        RuntimeStatusRequest.DeleteRuntime,
        () => (r) => r === null || r.status === RuntimeStatus.DELETED,
      ],
      [
        RuntimeStatusRequest.DeleteRuntimeAndPD,
        () => (r) => r === null || r.status === RuntimeStatus.DELETED,
      ],
      [
        RuntimeStatusRequest.DeletePD,
        () => (r) =>
          r.status === RuntimeStatus.RUNNING ||
          r.status === RuntimeStatus.STOPPED,
      ],
      [
        RuntimeStatusRequest.Start,
        () => (r) => r.status === RuntimeStatus.RUNNING,
      ],
      [
        RuntimeStatusRequest.Stop,
        () => (r) => r.status === RuntimeStatus.STOPPED,
      ]
    );
    const initializePolling = async () => {
      if (!!runtimeStatusRequest) {
        try {
          await LeoRuntimeInitializer.initialize({
            workspaceNamespace: currentWorkspaceNamespace,
            maxCreateCount: 0,
            resolutionCondition: (r) => resolutionCondition(r),
          });
        } catch (e) {
          // ExceededActionCountError is expected, as we exceed our create limit of 0.
          if (
            !(
              e instanceof ExceededActionCountError ||
              e instanceof LeoRuntimeInitializationAbortedError
            )
          ) {
            throw e;
          }
        }
      }
    };
    initializePolling();
  }, [runtimeStatusRequest]);

  const setStatusRequest = async (req: RuntimeStatusRequest) => {
    await switchCase<RuntimeStatusRequest, Promise<Response | EmptyResponse>>(
      req,
      [
        RuntimeStatusRequest.DeleteRuntime,
        () => {
          return runtimeApi().deleteRuntime(currentWorkspaceNamespace, false);
        },
      ],
      [
        RuntimeStatusRequest.DeleteRuntimeAndPD,
        () => {
          return runtimeApi().deleteRuntime(currentWorkspaceNamespace, true);
        },
      ],
      [
        RuntimeStatusRequest.DeletePD,
        () => {
          return disksApi().deleteDisk(
            currentWorkspaceNamespace,
            runtimeDiskStore.get().gcePersistentDisk.name
          );
        },
      ],
      [
        RuntimeStatusRequest.Start,
        () => {
          return leoRuntimesApi().startRuntime(
            currentGoogleProject,
            runtime.runtimeName
          );
        },
      ],
      [
        RuntimeStatusRequest.Stop,
        () => {
          return leoRuntimesApi().stopRuntime(
            currentGoogleProject,
            runtime.runtimeName
          );
        },
      ]
    );
    setRuntimeStatusRequest(req);
  };

  // runtimeStore may be outdated in certain scenarios; ensure we only return the
  // requested project so we aren't showing stale data.
  let status: RuntimeStatus;
  if (runtime?.googleProject === currentGoogleProject) {
    status = runtime?.status;
  }
  return [status, setStatusRequest];
};

// useCustomRuntime Hook can request a new runtime config
// The LeoRuntimeInitializer could potentially be rolled into this code to completely manage
// all runtime state.
export const useCustomRuntime = (
  currentWorkspaceNamespace: string,
  detachablePd: Disk | null
): [
  { currentRuntime: Runtime; pendingRuntime: Runtime },
  (request: { runtime: Runtime; detachedDisk: Disk | null }) => void
] => {
  const { runtime, workspaceNamespace } = useStore(runtimeStore);
  const runtimeOps = useStore(compoundRuntimeOpStore);
  const { pendingRuntime = null } = runtimeOps[currentWorkspaceNamespace] || {};
  const [request, setRequest] = useState<{
    runtime: Runtime;
    detachedDisk: Disk | null;
  }>();

  // Ensure that a runtime gets initialized, if it hasn't already been.
  useRuntime(currentWorkspaceNamespace);

  useEffect(() => {
    let mounted = true;
    const aborter = new AbortController();
    const existingDisk = runtimeDiskStore.get().gcePersistentDisk;
    const requestedDisk = request?.runtime?.gceWithPdConfig?.persistentDisk;
    const runAction = async () => {
      const applyRuntimeUpdate = async () => {
        const oldConfig = toAnalysisConfig(runtime, detachablePd);
        const newConfig = toAnalysisConfig(
          request.runtime,
          request.detachedDisk
        );
        const mostSevereDiff = findMostSevereDiffState(
          getAnalysisConfigDiffs(oldConfig, newConfig).map(({ diff }) => diff)
        );
        const mostSevereDiskDiff = findMostSevereDiffState(
          getAnalysisConfigDiffs(oldConfig, newConfig).map(
            ({ diskDiff }) => diskDiff
          )
        );

        // A disk update may be need in combination with a runtime update.
        if (mostSevereDiskDiff === AnalysisDiffState.CAN_UPDATE_IN_PLACE) {
          await disksApi().updateDisk(
            currentWorkspaceNamespace,
            existingDisk.name,
            requestedDisk.size
          );
        }

        if (mostSevereDiff === AnalysisDiffState.NEEDS_DELETE) {
          const deleteAttachedDisk =
            mostSevereDiskDiff === AnalysisDiffState.NEEDS_DELETE;
          await runtimeApi().deleteRuntime(
            currentWorkspaceNamespace,
            deleteAttachedDisk,
            {
              signal: aborter.signal,
            }
          );
        } else if (
          [
            AnalysisDiffState.CAN_UPDATE_WITH_REBOOT,
            AnalysisDiffState.CAN_UPDATE_IN_PLACE,
          ].includes(mostSevereDiff)
        ) {
          if (
            runtime.status === RuntimeStatus.RUNNING ||
            runtime.status === RuntimeStatus.STOPPED
          ) {
            await runtimeApi().updateRuntime(currentWorkspaceNamespace, {
              runtime: request.runtime,
            });
            // Calling updateRuntime will not immediately set the Runtime status to not Running so the
            // default initializer will resolve on its first call. The polling below first checks for the
            // non Running status before initializing the default one that checks for Running status
            await LeoRuntimeInitializer.initialize({
              workspaceNamespace,
              targetRuntime: request.runtime,
              resolutionCondition: (r) => r.status !== RuntimeStatus.RUNNING,
              pollAbortSignal: aborter.signal,
              overallTimeout: 1000 * 60, // The switch to a non running status should occur quickly
            });
          }
        }
      };

      try {
        if (isVisible(runtime?.status)) {
          await applyRuntimeUpdate();
        } else if (diskNeedsSizeIncrease(requestedDisk, existingDisk)) {
          await disksApi().updateDisk(
            currentWorkspaceNamespace,
            existingDisk.name,
            requestedDisk.size
          );
        }

        if (runtime?.status === RuntimeStatus.ERROR) {
          await runtimeApi().deleteRuntime(currentWorkspaceNamespace, false, {
            signal: aborter.signal,
          });
        }

        await LeoRuntimeInitializer.initialize({
          workspaceNamespace,
          targetRuntime: request.runtime,
          pollAbortSignal: aborter.signal,
        });
      } catch (e) {
        if (!(e instanceof LeoRuntimeInitializationAbortedError)) {
          throw e;
        }
      } finally {
        markCompoundRuntimeOperationCompleted(currentWorkspaceNamespace);
        if (mounted) {
          setRequest(undefined);
        }
      }
    };

    if (request !== undefined && !fp.equals(request.runtime, runtime)) {
      registerCompoundRuntimeOperation(currentWorkspaceNamespace, {
        pendingRuntime: request.runtime,
        aborter,
      });
      runAction();
    }

    // After dismount, we still want the above store modifications to occur.
    // However, we should not continue to mutate the now unmounted hook state -
    // this will result in React warnings.
    return () => {
      mounted = false;
    };
  }, [request]);

  return [{ currentRuntime: runtime, pendingRuntime }, setRequest];
};

export const withRuntimeStore = () => (WrappedComponent) => {
  return (props) => {
    const value = useStore(runtimeStore);
    // Ensure that a runtime gets initialized, if it hasn't already been.
    useRuntime(value.workspaceNamespace);
    useDisk(value.workspaceNamespace);

    return <WrappedComponent {...props} runtimeStore={value} />;
  };
};
