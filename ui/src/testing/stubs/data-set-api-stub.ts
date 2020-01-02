import {
  DataDictionaryEntry,
  DataSet,
  DataSetApi,
  DataSetCodeResponse,
  DataSetExportRequest,
  DataSetListResponse,
  DataSetPreviewRequest,
  DataSetPreviewResponse,
  DataSetRequest, DomainValuesResponse,
  EmptyResponse,
  KernelTypeEnum
} from 'generated/fetch';

export class DataSetApiStub extends DataSetApi {
  static stubDataSets(): DataSet[] {
    return [
      {
        id: 0,
        name: 'Stub Dataset',
        description: 'Stub Dataset',
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

  previewDataSetByDomain(workspaceNamespace: string,
    workspaceId: string, dataSetPreviewRequest: DataSetPreviewRequest): Promise<DataSetPreviewResponse> {
    return Promise.resolve({
      domain: dataSetPreviewRequest.domain,
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

  getDataDictionaryEntry(cdrVersionId: number, domain: string, domainValue: string): Promise<DataDictionaryEntry> {
    return Promise.resolve({
      description: 'datadictionary description',
      relevantOmopTable: 'condition_occurrence',
      fieldType: 'bigint',
      dataProvenance: 'Blah'
    });
  }

  public getValuesFromDomain(workspaceNamespace: string, workspaceId: string, domain: string)
    : Promise<DomainValuesResponse> {
    const domainValueItems = [];
    switch (domain) {
      case 'CONDITION':
        domainValueItems.push({value: 'Condition1'});
        domainValueItems.push({value: 'Condition2'});
        break;
      case 'MEASUREMENT':
        domainValueItems.push({value: 'Measurement1'});
        domainValueItems.push({value: 'Measurement2'});
        domainValueItems.push({value: 'Measurement3'});
        break;
      case 'DRUG':
        domainValueItems.push({value: 'Drug1'});
        break;
    }
    return Promise.resolve({items: domainValueItems});
  }
}
