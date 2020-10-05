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

import * as React from 'react';

const {useState, useEffect} = React;

// Hook used to manage runtime state.
// The runtime can be set to null to delete a runtime, or a new runtime config
// The LeoRuntimeInitializer could potentially be rolled into this code to completely manage
// all runtime state.
export const useRuntime = (initialWorkspaceNamespace) => {
  const {runtime, workspaceNamespace} = useStore(runtimeStore);
  const [requestedRuntime, setRequestedRuntime] = useState<Runtime|null>();

  useOnMount(() => {
    const getRuntime = async () => {
      const leoRuntime = await runtimeApi().getRuntime(initialWorkspaceNamespace)
      runtimeStore.set({
        workspaceNamespace: initialWorkspaceNamespace,
        runtime: leoRuntime 
      });
    }
    getRuntime();
  });

  useEffect(() => {
    const action = requestedRuntime === null 
    ? async () => {
        await runtimeApi().deleteRuntime(workspaceNamespace);
        await LeoRuntimeInitializer.initialize({workspaceNamespace, maxCreateCount: 0});
      }
    : async () => {
        await runtimeApi().deleteRuntime(workspaceNamespace);
        await LeoRuntimeInitializer.initialize({
          workspaceNamespace,
          runtime: requestedRuntime
        });
      }; 

    const runAction = async () => {
      await action();
      const currentRuntime = await runtimeApi().getRuntime(workspaceNamespace);
      if (initialWorkspaceNamespace === workspaceNamespace) {
        runtimeStore.set({        
          workspaceNamespace: initialWorkspaceNamespace,
          runtime: currentRuntime
        });
      }
    }

    requestedRuntime !== undefined && !fp.equals(requestedRuntime, runtime) && runAction();
  }, [requestedRuntime]);


  return [runtime, setRequestedRuntime]
}