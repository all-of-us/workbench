import {mount} from 'enzyme';
import * as React from 'react';

import {datasetApi, registerApiClient} from 'app/services/swagger-fetch-clients';
import {
  DatasetApi,
  DatasetRequest,
  KernelTypeEnum,
  PrePackagedConceptSetEnum,
  WorkspacesApi} from 'generated/fetch';
import {waitOneTickAndUpdate} from 'testing/react-test-helpers';
import {DatasetApiStub} from 'testing/stubs/data-set-api-stub';
import {WorkspacesApiStub} from 'testing/stubs/workspaces-api-stub';
import {DatasetExportRequest} from 'generated/fetch';
import {NewDatasetModal} from './new-dataset-modal';
import GenomicsAnalysisToolEnum = DatasetExportRequest.GenomicsAnalysisToolEnum;
import GenomicsDataTypeEnum = DatasetExportRequest.GenomicsDataTypeEnum;

const prePackagedConceptSet = PrePackagedConceptSetEnum.NONE;
const workspaceNamespace = 'workspaceNamespace';
const workspaceId = 'workspaceId';

let dataset;

const createNewDatasetModal = () => {
  return <NewDatasetModal
    closeFunction={() => {}}
    includesAllParticipants={false}
    selectedConceptSetIds={[]}
    selectedCohortIds={[]}
    selectedDomainValuePairs={[]}
    workspaceNamespace={workspaceNamespace}
    workspaceId={workspaceId}
    dataset={dataset}
    prePackagedConceptSet={prePackagedConceptSet}
    displayMicroarrayOptions={false}
    billingLocked={false}
  />;
};

describe('NewDatasetModal', () => {
  beforeEach(() => {
    window.open = jest.fn();
    dataset = undefined;
    registerApiClient(WorkspacesApi, new WorkspacesApiStub());
    registerApiClient(DatasetApi, new DatasetApiStub());
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('should render', () => {
    const wrapper = mount(createNewDatasetModal());
    expect(wrapper.exists()).toBeTruthy();
  });

  it('should only display code when button pressed', async() => {
    const wrapper = mount(createNewDatasetModal());
    expect(wrapper.find('[data-test-id="code-text-box"]').exists()).toBeFalsy();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="code-preview-button"]')
      .first().text()).toEqual('See Code Preview');
    wrapper.find('[data-test-id="code-preview-button"]').first().simulate('click');
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="code-preview-button"]')
      .first().text()).toEqual('Hide Preview');
  });

  it('should not allow submission if no name specified', async() => {
    const wrapper = mount(createNewDatasetModal());
    const createSpy = jest.spyOn(datasetApi(), 'createDataset');
    const exportSpy = jest.spyOn(datasetApi(), 'exportToNotebook');

    wrapper.find('[data-test-id="save-data-set"]').first().simulate('click');
    await waitOneTickAndUpdate(wrapper);
    expect(createSpy).not.toHaveBeenCalled();
    expect(exportSpy).not.toHaveBeenCalled();
  });

  it('should not export if export is not checked', async() => {
    const wrapper = mount(createNewDatasetModal());
    const createSpy = jest.spyOn(datasetApi(), 'createDataset');
    const exportSpy = jest.spyOn(datasetApi(), 'exportToNotebook');
    const nameStub = 'Dataset Name';

    wrapper.find('[data-test-id="data-set-name-input"]')
      .first().simulate('change', {target: {value: nameStub}});
    await waitOneTickAndUpdate(wrapper);
    wrapper.find('[data-test-id="export-to-notebook"]').first().simulate('change', {target: {checked: false}});
    await waitOneTickAndUpdate(wrapper);
    wrapper.find('[data-test-id="save-data-set"]').first().simulate('click');
    await waitOneTickAndUpdate(wrapper);
    expect(createSpy).toHaveBeenCalledWith(workspaceNamespace, workspaceId, {
      name: nameStub,
      includesAllParticipants: false,
      description: '',
      conceptSetIds: [],
      cohortIds: [],
      domainValuePairs: [],
      prePackagedConceptSet: PrePackagedConceptSetEnum.NONE
    });
    expect(exportSpy).not.toHaveBeenCalled();
  });

  it('should not submit if export checked but no name or selected notebook', async() => {
    const wrapper = mount(createNewDatasetModal());
    const createSpy = jest.spyOn(datasetApi(), 'createDataset');
    const exportSpy = jest.spyOn(datasetApi(), 'exportToNotebook');
    const nameStub = 'Dataset Name';

    wrapper.find('[data-test-id="data-set-name-input"]')
      .first().simulate('change', {target: {value: nameStub}});
    await waitOneTickAndUpdate(wrapper);
    wrapper.find('[data-test-id="save-data-set"]').first().simulate('click');
    await waitOneTickAndUpdate(wrapper);
    expect(createSpy).not.toHaveBeenCalled();
    expect(exportSpy).not.toHaveBeenCalled();
  });

  it('should submit if export is unchecked and name specified', async() => {
    const wrapper = mount(createNewDatasetModal());
    const createSpy = jest.spyOn(datasetApi(), 'createDataset');
    const exportSpy = jest.spyOn(datasetApi(), 'exportToNotebook');
    const nameStub = 'Dataset Name';
    const notebookNameStub = 'Notebook Name';
    const datasetRequestStub: DatasetRequest = {
      name: nameStub,
      includesAllParticipants: false,
      description: '',
      conceptSetIds: [],
      cohortIds: [],
      domainValuePairs: [],
      prePackagedConceptSet: PrePackagedConceptSetEnum.NONE
    };

    wrapper.find('[data-test-id="data-set-name-input"]')
      .first().simulate('change', {target: {value: nameStub}});
    await waitOneTickAndUpdate(wrapper);
    wrapper.find('[data-test-id="notebook-name-input"]')
      .first().simulate('change', {target: {value: notebookNameStub}});
    await waitOneTickAndUpdate(wrapper);
    wrapper.find('[data-test-id="save-data-set"]').first().simulate('click');
    await waitOneTickAndUpdate(wrapper);
    expect(createSpy).toHaveBeenCalled();
    expect(exportSpy).toHaveBeenCalledWith(workspaceNamespace, workspaceId, {
      datasetRequest: datasetRequestStub,
      newNotebook: true,
      notebookName: notebookNameStub,
      kernelType: KernelTypeEnum.Python,
      genomicsDataType: GenomicsDataTypeEnum.NONE,
      genomicsAnalysisTool: GenomicsAnalysisToolEnum.NONE
    });
  });

  it ('should have default dataset name if dataset is passed as props', () => {
    const name = 'Update Dataset';
    dataset = {...dataset, name: name, description: 'dataset'};
    const wrapper = mount(createNewDatasetModal());
    const datasetName  =
      wrapper.find('[data-test-id="data-set-name-input"]').first().prop('value');
    expect(datasetName).toBe(name);
  });

  it ('should show microarray options if the display flag is true and the kernel is Python', async() => {
    const wrapper = mount(createNewDatasetModal());
    wrapper.setProps({displayMicroarrayOptions: true});
    wrapper.setState({kernelType: KernelTypeEnum.Python});
    await waitOneTickAndUpdate(wrapper);

    expect(wrapper.find('[data-test-id="include-raw-microarray-data"]').exists()).toBeTruthy();
  });

  it ('should not show microarray options if the cdrVersion does not have microarray data', async() => {
    const wrapper = mount(createNewDatasetModal());
    wrapper.setProps({displayMicroarrayOptions: false});
    wrapper.setState({kernelType: KernelTypeEnum.Python});
    await waitOneTickAndUpdate(wrapper);

    expect(wrapper.find('[data-test-id="include-raw-microarray-data"]').exists()).toBeFalsy();
  });

  it ('should not show microarray options if the kernel is not Python', async() => {
    const wrapper = mount(createNewDatasetModal());
    wrapper.setProps({displayMicroarrayOptions: true});
    wrapper.setState({kernelType: KernelTypeEnum.R});
    await waitOneTickAndUpdate(wrapper);

    expect(wrapper.find('[data-test-id="include-raw-microarray-data"]').exists()).toBeFalsy();
  });

  it ('should show genomics analysis tools if include raw microarray data is checked', async() => {
    const wrapper = mount(createNewDatasetModal());
    wrapper.setProps({displayMicroarrayOptions: true});
    wrapper.setState({kernelType: KernelTypeEnum.Python, includeRawMicroarrayData: true});
    await waitOneTickAndUpdate(wrapper);

    expect(wrapper.find('[data-test-id="genomics-analysis-tool-none"]').exists()).toBeTruthy();
    expect(wrapper.find('[data-test-id="genomics-analysis-tool-hail"]').exists()).toBeTruthy();
    expect(wrapper.find('[data-test-id="genomics-analysis-tool-plink"]').exists()).toBeTruthy();
  });

  it ('switching to R should uncheck microarray option', async() => {
    const wrapper = mount(createNewDatasetModal());
    wrapper.setProps({displayMicroarrayOptions: true});
    wrapper.setState({kernelType: KernelTypeEnum.Python, includeRawMicroarrayData: true});
    await waitOneTickAndUpdate(wrapper);

    wrapper.find('[data-test-id="kernel-type-r"]').first().simulate('click');
    await waitOneTickAndUpdate(wrapper);

    expect(wrapper.instance().state['includeRawMicroarrayData']).toBeFalsy();
  });

  it ('should export to notebook with the correct microarray parameters', async() => {
    const wrapper = mount(createNewDatasetModal());
    wrapper.setProps({displayMicroarrayOptions: true});
    wrapper.setState({kernelType: KernelTypeEnum.Python});

    const exportSpy = jest.spyOn(datasetApi(), 'exportToNotebook');
    const nameStub = 'Dataset Name';
    const notebookNameStub = 'Notebook Name';
    const datasetRequestStub: DatasetRequest = {
      name: nameStub,
      includesAllParticipants: false,
      description: '',
      conceptSetIds: [],
      cohortIds: [],
      domainValuePairs: [],
      prePackagedConceptSet: PrePackagedConceptSetEnum.NONE
    };

    wrapper.find('[data-test-id="data-set-name-input"]')
      .first().simulate('change', {target: {value: nameStub}});
    await waitOneTickAndUpdate(wrapper);

    wrapper.find('[data-test-id="notebook-name-input"]')
      .first().simulate('change', {target: {value: notebookNameStub}});
    await waitOneTickAndUpdate(wrapper);

    wrapper.find('[data-test-id="include-raw-microarray-data"]')
      .first().simulate('change', {target: {checked: true}});
    await waitOneTickAndUpdate(wrapper);

    wrapper.find('[data-test-id="genomics-analysis-tool-hail"]')
      .first().simulate('click');
    await waitOneTickAndUpdate(wrapper);

    wrapper.find('[data-test-id="save-data-set"]').first().simulate('click');
    await waitOneTickAndUpdate(wrapper);

    expect(exportSpy).toHaveBeenCalledWith(workspaceNamespace, workspaceId, {
      datasetRequest: datasetRequestStub,
      newNotebook: true,
      notebookName: notebookNameStub,
      kernelType: KernelTypeEnum.Python,
      genomicsDataType: GenomicsDataTypeEnum.MICROARRAY,
      genomicsAnalysisTool: GenomicsAnalysisToolEnum.HAIL
    });
  });

});
