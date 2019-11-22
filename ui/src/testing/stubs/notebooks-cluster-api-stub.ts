import {ClusterApi} from 'notebooks-generated/fetch';


export class NotebooksClusterApiStub extends ClusterApi {

  constructor() {
    super(undefined, undefined, (..._: any[]) => {
      throw Error('cannot fetch in tests');
    });
  }

  public startCluster(googleProject: string, clusterName: string,
    extraHttpRequestParams?: any): Promise<Response> {
    return new Promise<Response>(resolve => {
      resolve(new Response());
    });
  }

}
