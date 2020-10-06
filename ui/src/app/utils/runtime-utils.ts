import {
  runtimeStore,
  useStore
} from 'app/utils/stores';
import {useOnMount} from 'app/utils';
import {
  LeoRuntimeInitializer,
} from 'app/utils/leo-runtime-initializer';
import {Runtime} from 'generated/fetch';
import {runtimeApi} from 'app/services/swagger-fetch-clients';
import * as fp from 'lodash/fp';
import {switchCase} from 'app/utils';

import * as React from 'react';

const {useState, useEffect} = React;

export type RuntimeStatusRequest = 'Delete';

export const RuntimeStateRequest= {
    Delete: 'Delete' as RuntimeStatusRequest
};

// useRuntime hook is a simple hook to populate the runtime store.
// This is only used by other runtime hooks
const useRuntime = (currentWorkspaceNamespace) => {
  useOnMount(() => {
    const getRuntime = async () => {
      const leoRuntime = await runtimeApi().getRuntime(currentWorkspaceNamespace)
      runtimeStore.set({
        workspaceNamespace: currentWorkspaceNamespace,
        runtime: leoRuntime 
      });
    }
    getRuntime();
  });
}

// useRuntimeState hook can be used to change the state of the runtime
// Only 'Delete' is supported at the moment
export const useRuntimeState = (currentWorkspaceNamespace): [RuntimeStatusRequest, Function]  => {
  const [requestedRuntimeState, setRuntimeState] = useState<RuntimeStatusRequest>();
  useRuntime(currentWorkspaceNamespace)

  useEffect(() => {
    // Additional state changes can be put here
    !!requestedRuntimeState && switchCase(requestedRuntimeState, 
      [RuntimeStateRequest.Delete, async () => {
        await runtimeApi().deleteRuntime(currentWorkspaceNamespace);
        await LeoRuntimeInitializer.initialize({workspaceNamespace: currentWorkspaceNamespace, maxCreateCount: 0});
      }])
    
  }, [requestedRuntimeState])

  return [requestedRuntimeState, setRuntimeState]
}

// useCustomRuntime Hook can request a new runtime config
// The LeoRuntimeInitializer could potentially be rolled into this code to completely manage
// all runtime state.
export const useCustomRuntime = (currentWorkspaceNamespace): [Runtime, Function] => {
  const {runtime, workspaceNamespace} = useStore(runtimeStore);
  const [requestedRuntime, setRequestedRuntime] = useState<Runtime>();
  useRuntime(currentWorkspaceNamespace)

  useEffect(() => {
    const runAction = async () => {
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
    }

    requestedRuntime !== undefined && !fp.equals(requestedRuntime, runtime) && runAction();
  }, [requestedRuntime]);


  return [runtime, setRequestedRuntime]
}