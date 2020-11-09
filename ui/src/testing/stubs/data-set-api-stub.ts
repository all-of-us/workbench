import {
  DataDictionaryEntry,
  Dataset,
  DatasetApi,
  DatasetCodeResponse,
  DatasetExportRequest,
  DatasetPreviewRequest,
  DatasetPreviewResponse,
  DatasetRequest,
  DomainValuesResponse,
  EmptyResponse,
  KernelTypeEnum,
  MarkDatasetRequest
} from 'generated/fetch';
import {stubNotImplementedError} from 'testing/stubs/stub-utils';

export class DatasetApiStub extends DatasetApi {
  static stubDatasets(): Dataset[] {
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
    super(undefined, undefined, (..._: any[]) => { throw stubNotImplementedError; });
  }

  generateCode(workspaceNamespace: string,
    workspaceId: string,
    kernelType: string,
    dataset: DatasetRequest): Promise<DatasetCodeResponse> {
    return new Promise<DatasetCodeResponse>(resolve => {
      resolve({kernelType: KernelTypeEnum[kernelType], code: ''});
    });
  }

  createDataset(workspaceNamespace: string,
    workspaceId: string,
    dataset: DatasetRequest): Promise<Dataset> {
    return new Promise<Dataset>(resolve => {
      resolve({});
    });
  }

  exportToNotebook(workspaceNamespace: string,
    workspaceId: string,
    datasetExportRequest: DatasetExportRequest): Promise<EmptyResponse> {
    return new Promise<EmptyResponse>(resolve => {
      resolve({});
    });
  }

  previewDatasetByDomain(workspaceNamespace: string,
    workspaceId: string, datasetPreviewRequest: DatasetPreviewRequest): Promise<DatasetPreviewResponse> {
    return Promise.resolve({
      domain: datasetPreviewRequest.domain,
      values: [
        {value: 'Value1', queryValue: ['blah']},
        {value: 'Value2', queryValue: ['blah2']}
      ]
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

  public getValuesFromDomain(workspaceNamespace: string, workspaceId: string, domain: string): Promise<DomainValuesResponse> {
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

  async markDirty(workspaceNamespace: string, workspaceId: string, markDatasetRequest?: MarkDatasetRequest, options?: any) {
    return true;
  }
}
