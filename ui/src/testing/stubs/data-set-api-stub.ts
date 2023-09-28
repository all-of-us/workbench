import {
  DataDictionaryEntry,
  DataSet,
  DataSetApi,
  DataSetCodeResponse,
  DataSetPreviewRequest,
  DataSetPreviewResponse,
  DomainValuesResponse,
  EmptyResponse,
  KernelTypeEnum,
  ReadOnlyNotebookResponse,
} from 'generated/fetch';

export const stubDataSet = (): DataSet => ({
  id: 1,
  name: 'Stub Dataset',
  description: 'Stub Dataset',
  includesAllParticipants: false,
  workspaceId: 1,
  lastModifiedTime: 10000,
  conceptSets: [],
  cohorts: [],
  domainValuePairs: [],
  prePackagedConceptSet: [],
});

export class DataSetApiStub extends DataSetApi {
  public codePreview;
  public getDatasetMock = stubDataSet();
  public extractionJobs;

  static stubDataSets(): DataSet[] {
    return [stubDataSet()];
  }

  constructor() {
    super(undefined);
    this.extractionJobs = [];
  }

  getDataSet(): Promise<DataSet> {
    return new Promise<DataSet>((resolve) => resolve(this.getDatasetMock));
  }

  generateCode(
    workspaceNamespace: string,
    workspaceId: string,
    kernelType: string
  ): Promise<DataSetCodeResponse> {
    return new Promise<DataSetCodeResponse>((resolve) => {
      resolve({ kernelType: KernelTypeEnum[kernelType], code: '' });
    });
  }

  previewExportToNotebook(): Promise<ReadOnlyNotebookResponse> {
    return new Promise<ReadOnlyNotebookResponse>((resolve) => {
      resolve(this.codePreview);
    });
  }

  createDataSet(): Promise<DataSet> {
    return new Promise<DataSet>((resolve) => {
      resolve({});
    });
  }

  exportToNotebook(): Promise<EmptyResponse> {
    return new Promise<EmptyResponse>((resolve) => {
      resolve({});
    });
  }

  previewDataSetByDomain(
    workspaceNamespace: string,
    workspaceId: string,
    dataSetPreviewRequest: DataSetPreviewRequest
  ): Promise<DataSetPreviewResponse> {
    return Promise.resolve({
      domain: dataSetPreviewRequest.domain,
      values: [
        { value: 'Value1', queryValue: ['blah'] },
        { value: 'Value2', queryValue: ['blah2'] },
      ],
    });
  }

  getDataDictionaryEntry(): Promise<DataDictionaryEntry> {
    return Promise.resolve({
      description: 'datadictionary description',
      relevantOmopTable: 'condition_occurrence',
      fieldType: 'bigint',
      dataProvenance: 'Blah',
    });
  }

  public getValuesFromDomain(
    workspaceNamespace: string,
    workspaceId: string,
    domain: string
  ): Promise<DomainValuesResponse> {
    const domainValueItems = [];
    switch (domain) {
      case 'CONDITION':
        domainValueItems.push({ value: 'Condition1' });
        domainValueItems.push({ value: 'Condition2' });
        break;
      case 'MEASUREMENT':
        domainValueItems.push({ value: 'Measurement1' });
        domainValueItems.push({ value: 'Measurement2' });
        domainValueItems.push({ value: 'Measurement3' });
        break;
      case 'DRUG':
        domainValueItems.push({ value: 'Drug1' });
        break;
    }
    return Promise.resolve({
      items: [
        {
          domain,
          items: domainValueItems,
        },
      ],
    });
  }

  async markDirty() {
    return true;
  }

  async getGenomicExtractionJobs() {
    return Promise.resolve({ jobs: this.extractionJobs });
  }
}
