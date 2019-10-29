import {
  CdrVersionListResponse,
  Cluster,
  ClusterApi,
  ClusterListResponse,
  ClusterLocalizeRequest,
  ClusterLocalizeResponse,
  ClusterStatus,
  DataAccessLevel
} from 'generated/fetch';

export class ClusterApiStub extends ClusterApi {
  public cluster: Cluster;

  constructor() {
    super(undefined, undefined, (..._: any[]) => { throw Error('cannot fetch in tests'); });
    this.cluster = {
      clusterName: 'Cluster Name',
      clusterNamespace: 'Namespace',
      status: ClusterStatus.Running,
      createdDate: '08/08/2018'
    };
  }

  listClusters(extraHttpRequestParams?: any): Promise<ClusterListResponse> {
    return new Promise<ClusterListResponse>(resolve => {
      console.log(this.cluster);
      resolve({defaultCluster: this.cluster});
    });
  }

  deleteCluster(projectName: string, clusterName: string,
    extraHttpRequestParams?: any): Promise<{}> {
    return new Promise<{}>(resolve => {
      this.cluster.status = ClusterStatus.Deleting;
      resolve({});
    });
  }


  getCdrVersions(): Promise<CdrVersionListResponse> {
    let cdrVersionList: CdrVersionListResponse;
    cdrVersionList = <CdrVersionListResponse>{
      items: [{
        cdrVersionId: '1',
        name: 'Cdr version1',
        dataAccessLevel: DataAccessLevel.Registered,
        creationTime: 123
      }, {
        cdrVersionId: '2',
        name: 'Cdr version2',
        dataAccessLevel: DataAccessLevel.Unregistered,
        creationTime: 456
      }, {
        cdrVersionId: '3',
        name: 'Cdr version3',
        dataAccessLevel: DataAccessLevel.Registered,
        creationTime: 789
      }],
      defaultCdrVersionId: '1',
    };

    return new Promise<CdrVersionListResponse>(resolve => {
      resolve(cdrVersionList);
    });
  }

  localize(projectName: string, clusterName: string, req: ClusterLocalizeRequest,
    extraHttpRequestParams?: any): Promise<ClusterLocalizeResponse> {
    return new Promise<ClusterLocalizeResponse>(resolve => {
      resolve({clusterLocalDirectory: 'workspaces/${req.workspaceId}'});
    });
  }
}
