import {UserMetricsApi} from 'generated/fetch';

export class UserMetricsApiStub extends UserMetricsApi {
  constructor() {
    super(undefined, undefined, (..._: any[]) => { throw Error('cannot fetch in tests'); });
  }

  getUserRecentResources() {
    return Promise.resolve([
      {
        modifiedTime: '2019-01-28 20:13:58.0',
        notebook: {lastModifiedTime: null, name: 'a.ipynb', path: 'gs://q/notebooks/'},
        permission: 'OWNER',
        workspaceFirecloudName: 'q',
        workspaceId: 1,
        workspaceNamespace: 'z'
      }
    ]);
  }
}
