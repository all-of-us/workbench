import {
  DataSet,
  DataSetApi,
  DataSetExportRequest,
  DataSetListResponse,
  DataSetQueryList,
  DataSetRequest,
  EmptyResponse
} from 'generated/fetch';

export class DataSetApiStub extends DataSetApi {
  static stubDataSets(): DataSet[] {
    return [
      {
        id: 0,
        name: 'Stub Data Set',
        description: 'Stub Data Set',
        includesAllParticipants: false,
        workspaceId: 0,
        lastModifiedTime: 10000,
        conceptSets: [],
        cohorts: [],
        values: []
      }
    ];
  }

  constructor() {
    super(undefined, undefined, (..._: any[]) => { throw Error('cannot fetch in tests'); });
  }

  generateQuery(workspaceNamespace: string,
    workspaceId: string,
    dataSet: DataSetRequest): Promise<DataSetQueryList> {
    return new Promise<DataSetQueryList>(resolve => {
      resolve({queryList: []});
    });
  }

  createDataSet(workspaceNamespace: string,
    workspaceId: string,
    dataSet: DataSetRequest): Promise<DataSet> {
    return new Promise<DataSet>(resolve => {
      resolve({});
    });
  }

  exportToNotebook(workspaceNamespace: string,
    workspaceId: string,
    dataSetExportRequest: DataSetExportRequest): Promise<EmptyResponse> {
    return new Promise<EmptyResponse>(resolve => {
      resolve({});
    });
  }

  getDataSetsInWorkspace(
    workspaceNamespace: string,
    workspaceId: string): Promise<DataSetListResponse> {
    return new Promise<DataSetListResponse>(resolve => {
      resolve({items: DataSetApiStub.stubDataSets()});
    });
  }
}
