import {urlParamsStore} from './navigation';

let pollAborter = new AbortController();

console.log(urlParamsStore);

// urlParamsStore
//   .map(({ns, wsid}) => ({ns, wsid}))
//   .distinctUntilChanged(fp.isEqual)
//   .switchMap(({ns, wsid}) => {
//     // This needs to happen for testing because we seed the urlParamsStore with {}.
//     // Otherwise it tries to make an api call with undefined, because the component
//     // initializes before we have access to the route.
//     if (ns === undefined || wsid === undefined) {
//       return Promise.resolve(null);
//     }
//
//     // In a handful of situations - namely on workspace creation/clone,
//     // the application will preload the next workspace to avoid a redundant
//     // refetch here.
//     const nextWs = nextWorkspaceWarmupStore.getValue();
//     nextWorkspaceWarmupStore.next(undefined);
//     if (nextWs && nextWs.namespace === ns && nextWs.id === wsid) {
//       return Promise.resolve(nextWs);
//     }
//     return workspacesApi().getWorkspace(ns, wsid).then((wsResponse) => {
//       return {
//         ...wsResponse.workspace,
//         accessLevel: wsResponse.accessLevel
//       };
//     });
//   })
//   .subscribe(async(workspace) => {
//     if (workspace === null) {
//       // This handles the empty urlParamsStore story.
//       return;
//     }
//     this.workspace = workspace;
//     console.log('setting store through url params');
//     currentWorkspaceStore.next(workspace);
//     runtimeStore.set({workspaceNamespace: workspace.namespace, runtime: undefined});
//     pollAborter.abort();
//     pollAborter = new AbortController();
//     try {
//       await LeoRuntimeInitializer.initialize({
//         workspaceNamespace: workspace.namespace,
//         pollAbortSignal: pollAborter.signal,
//         maxCreateCount: 0,
//         maxDeleteCount: 0,
//         maxResumeCount: 0
//       });
//     } catch (e) {
//       // Ignore ExceededActionCountError. This is thrown when the runtime doesn't exist, or
//       // isn't started. Both of these scenarios are expected, since we don't want to do any lazy
//       // initialization here.
//       if (!(e instanceof ExceededActionCountError)) {
//         throw e;
//       }
//     }
//   });
