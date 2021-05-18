import {
  DataDictionaryEntry,
  DataSet,
  DataSetApi,
  DataSetCodeResponse,
  DataSetExportRequest,
  DataSetPreviewRequest,
  DataSetPreviewResponse,
  DataSetRequest,
  DomainValuesResponse,
  EmptyResponse,
  KernelTypeEnum,
  MarkDataSetRequest, ReadOnlyNotebookResponse
} from 'generated/fetch';
import {stubNotImplementedError} from 'testing/stubs/stub-utils';

export const stubDataSet = (): DataSet => ({
  id: 0,
  name: 'Stub Dataset',
  description: 'Stub Dataset',
  includesAllParticipants: false,
  workspaceId: 0,
  lastModifiedTime: 10000,
  conceptSets: [],
  cohorts: [],
  domainValuePairs: []
});

export class DataSetApiStub extends DataSetApi {
  public codePreview;

  static stubDataSets(): DataSet[] {
    return [stubDataSet()];
  }

  constructor() {
    super(undefined, undefined, (..._: any[]) => { throw stubNotImplementedError; });
  }

  generateCode(workspaceNamespace: string,
    workspaceId: string,
    kernelType: string,
    dataSet: DataSetRequest): Promise<DataSetCodeResponse> {
    return new Promise<DataSetCodeResponse>(resolve => {
      resolve({kernelType: KernelTypeEnum[kernelType], code: ''});
    });
  }

  previewExportToNotebook(workspaceNamespace: string, workspaceId: string, dataSetExportRequest: DataSetExportRequest, options?: any): Promise<ReadOnlyNotebookResponse> {
    return new Promise<ReadOnlyNotebookResponse>(resolve => {
      resolve(this.codePreview);
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

  async markDirty(workspaceNamespace: string, workspaceId: string, markDataSetRequest?: MarkDataSetRequest, options?: any) {
    return true;
  }
}
