import {runtimeApi} from 'app/services/swagger-fetch-clients';
import {switchCase} from 'app/utils';
import { withErrorHandling } from 'app/utils';
import {
  LeoRuntimeInitializer,
} from 'app/utils/leo-runtime-initializer';
import {
  runtimeStore,
  useStore
} from 'app/utils/stores';
import {Runtime, RuntimeStatus} from 'generated/fetch';
import * as fp from 'lodash/fp';

import * as React from 'react';

const {useState, useEffect} = React;

export enum RuntimeStatusRequest {
  Delete = 'Delete'
}

// useRuntime hook is a simple hook to populate the runtime store.
// This is only used by other runtime hooks
const useRuntime = (currentWorkspaceNamespace) => {
  useEffect(() => {
    const getRuntime = withErrorHandling(
      () => runtimeStore.set({workspaceNamespace: null, runtime: null}),
      async() => {
        const leoRuntime = await runtimeApi().getRuntime(currentWorkspaceNamespace);
        runtimeStore.set({
          workspaceNamespace: currentWorkspaceNamespace,
          runtime: leoRuntime
        });
      });

    if (currentWorkspaceNamespace !== runtimeStore.get().workspaceNamespace) {
      runtimeStore.set({workspaceNamespace: currentWorkspaceNamespace, runtime: undefined});
      getRuntime();
    }
  }, []);
};

// useRuntimeState hook can be used to change the state of the runtime
// Only 'Delete' is supported at the moment
export const useRuntimeState = (currentWorkspaceNamespace): [RuntimeStatus | undefined, Function]  => {
  const [runtimeStatus, setRuntimeState] = useState<RuntimeStatusRequest>();
  const {runtime} = useStore(runtimeStore);
  useRuntime(currentWorkspaceNamespace);

  useEffect(() => {
    // Additional state changes can be put here
    if (!!runtimeStatus) {
      switchCase(runtimeStatus,
        [RuntimeStatusRequest.Delete, async() => {
          await runtimeApi().deleteRuntime(currentWorkspaceNamespace);
          await LeoRuntimeInitializer.initialize({workspaceNamespace: currentWorkspaceNamespace, maxCreateCount: 0});
        }]);
    }

  }, [runtimeStatus]);

  return [runtime ? runtime.status : undefined, setRuntimeState];
};

// useCustomRuntime Hook can request a new runtime config
// The LeoRuntimeInitializer could potentially be rolled into this code to completely manage
// all runtime state.
export const useCustomRuntime = (currentWorkspaceNamespace): [Runtime, Function] => {
  const {runtime, workspaceNamespace} = useStore(runtimeStore);
  const [requestedRuntime, setRequestedRuntime] = useState<Runtime>();
  useRuntime(currentWorkspaceNamespace);

  useEffect(() => {
    const runAction = async() => {
      await runtimeApi().deleteRuntime(currentWorkspaceNamespace);
      await LeoRuntimeInitializer.initialize({
        workspaceNamespace,
        runtime: requestedRuntime
      });

      const currentRuntime = await runtimeApi().getRuntime(currentWorkspaceNamespace);
      if (currentWorkspaceNamespace === workspaceNamespace) {
        runtimeStore.set({
          workspaceNamespace: currentWorkspaceNamespace,
          runtime: currentRuntime
        });
      }
    };

    if (requestedRuntime !== undefined && !fp.equals(requestedRuntime, runtime)) {
      runAction();
    }
  }, [requestedRuntime]);

  return [runtime, setRequestedRuntime];
};
