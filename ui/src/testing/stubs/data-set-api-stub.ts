import {
  DataSet,
  DataSetApi,
  DataSetCodeResponse,
  DataSetExportRequest,
  DataSetListResponse,
  DataSetPreviewRequest,
  DataSetPreviewResponse,
  DataSetRequest,
  EmptyResponse,
  KernelTypeEnum
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
        domainValuePairs: []
      }
    ];
  }

  constructor() {
    super(undefined, undefined, (..._: any[]) => { throw Error('cannot fetch in tests'); });
  }

  generateCode(workspaceNamespace: string,
    workspaceId: string,
    kernelType: string,
    dataSet: DataSetRequest): Promise<DataSetCodeResponse> {
    return new Promise<DataSetCodeResponse>(resolve => {
      resolve({kernelType: KernelTypeEnum[kernelType], code: ''});
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

  generateDataSetPreview(workspaceNamespace: string,
    workspaceId: string, dataSetPreviewRequest: DataSetPreviewRequest): Promise<DataSetPreviewResponse> {
    return Promise.resolve({
      values: [
        {value: 'Value1', queryValue: ['blah']},
        {value: 'Value2', queryValue: ['blah2']}
      ]
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
