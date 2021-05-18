import {mount} from 'enzyme';
import * as React from 'react';

import {dataSetApi, registerApiClient} from 'app/services/swagger-fetch-clients';
import {
  DataSetApi,
  DataSetRequest,
  KernelTypeEnum,
  WorkspacesApi} from 'generated/fetch';
import {waitOneTickAndUpdate} from 'testing/react-test-helpers';
import {DataSetApiStub} from 'testing/stubs/data-set-api-stub';
import {WorkspacesApiStub} from 'testing/stubs/workspaces-api-stub';
import {workspaceDataStub} from '../../../../testing/stubs/workspaces';
import {Select} from '../../../components/inputs';
import {Tooltip} from '../../../components/popups';
import {currentWorkspaceStore} from '../../../utils/navigation';
import {ExportDatasetModal} from './export-dataset-modal';

describe('ExportDatasetModal', () => {
  let dataset;
  let workspace;
  let testProps;
  let workspacesApiStub;
  let datasetApiStub;

  const component = (props) => {
    return <ExportDatasetModal {...props}/>;
  };

  function findExportButton(wrapper) {
    return wrapper.find('[data-test-id="export-data-set"]').first();
  }

  beforeEach(() => {
    window.open = jest.fn();
    dataset = {
      id: 1,
      name: 'Test Dataset Name'
    };

    workspacesApiStub = new WorkspacesApiStub();
    registerApiClient(WorkspacesApi, workspacesApiStub);
    datasetApiStub = new DataSetApiStub();
    registerApiClient(DataSetApi, datasetApiStub);

    workspace = workspaceDataStub;
    currentWorkspaceStore.next(workspace);

    testProps = {
      closeFunction: () => {},
      dataset: dataset
    };
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('should render', () => {
    const wrapper = mount(component(testProps));
    expect(wrapper.exists()).toBeTruthy();
  });

  it('should export to a new notebook', async() => {
    const wrapper = mount(component(testProps));
    const exportSpy = jest.spyOn(dataSetApi(), 'exportToNotebook');
    const expectedNotebookName = 'Notebook Name';
    const expectedDatasetRequest: DataSetRequest = {
      dataSetId: dataset.id,
      name: dataset.name
    };

    wrapper.find('[data-test-id="notebook-name-input"]')
      .first().simulate('change', {target: {value: expectedNotebookName}});
    findExportButton(wrapper).simulate('click');
    expect(exportSpy).toHaveBeenCalledWith(workspace.namespace, workspace.id, {
      dataSetRequest: expectedDatasetRequest,
      newNotebook: true,
      notebookName: expectedNotebookName,
      kernelType: KernelTypeEnum.Python
    });
  });


  it('should disable export if no name is provided', async() => {
    const wrapper = mount(component(testProps));
    const exportSpy = jest.spyOn(dataSetApi(), 'exportToNotebook');

    wrapper.find('[data-test-id="notebook-name-input"]').first()
      .simulate('change', {target: {value: ''}});
    findExportButton(wrapper).simulate('click');
    expect(findExportButton(wrapper).props().disabled).toBeTruthy();
    expect(exportSpy).not.toHaveBeenCalled();

    findExportButton(wrapper).simulate('mouseenter');
    expect(wrapper.find(Tooltip).text()).toBe('Notebook name can\'t be blank');
  });

  it('should disable export if a conflicting name is provided', async() => {
    workspacesApiStub.notebookList = [
      {
        'name': 'existing notebook.ipynb'
      }];
    const wrapper = mount(component(testProps));
    const exportSpy = jest.spyOn(dataSetApi(), 'exportToNotebook');

    wrapper.find('[data-test-id="notebook-name-input"]')
      .first().simulate('change', {target: {value: 'existing notebook'}});
    await waitOneTickAndUpdate(wrapper);
    findExportButton(wrapper).simulate('click');
    expect(findExportButton(wrapper).props().disabled).toBeTruthy();

    findExportButton(wrapper).simulate('mouseenter');
    expect(wrapper.find(Tooltip).text()).toBe('Notebook name already exists');
    expect(exportSpy).not.toHaveBeenCalled();
  });

  it('should export to an existing notebook with the correct kernel type', async() => {
    const expectedNotebookName = 'existing notebook';
    dataset.name = expectedNotebookName;
    workspacesApiStub.notebookList = [
      {
        'name': `${expectedNotebookName}.ipynb`
      }];
    workspacesApiStub.notebookKernel = KernelTypeEnum.R;

    const expectedDatasetRequest = {
      dataSetId: dataset.id,
      name: expectedNotebookName
    };

    const wrapper = mount(component(testProps));
    const exportSpy = jest.spyOn(dataSetApi(), 'exportToNotebook');

    wrapper.find(Select).props().onChange(expectedNotebookName);
    await waitOneTickAndUpdate(wrapper);

    findExportButton(wrapper).simulate('click');

    expect(exportSpy).toHaveBeenCalledWith(workspace.namespace, workspace.id, {
      dataSetRequest: expectedDatasetRequest,
      newNotebook: false,
      notebookName: expectedNotebookName,
      kernelType: KernelTypeEnum.R
    });
  });

  it('should show code preview, auto reload on kernel switch, and hide code preview', async() => {
    const expectedDatasetRequest = {
      dataSetId: dataset.id,
      name: dataset.name
    };
    datasetApiStub.codePreview = {
      html: '<div id="notebook">print("hello world!")</div>'
    };
    const wrapper = mount(component(testProps));
    const previewSpy = jest.spyOn(dataSetApi(), 'previewExportToNotebook');

    wrapper.find('[data-test-id="code-preview-button"]').simulate('click');
    await waitOneTickAndUpdate(wrapper);

    expect(previewSpy).toHaveBeenCalledWith(workspace.namespace, workspace.id, expect.objectContaining({
      dataSetRequest: expectedDatasetRequest,
      kernelType: KernelTypeEnum.Python
    }));
    expect(wrapper.find('iframe').html()).toContain('hello world!');

    wrapper.find('[data-test-id="kernel-type-r"]').first().simulate('click');
    await waitOneTickAndUpdate(wrapper);
    expect(previewSpy).toHaveBeenCalledWith(workspace.namespace, workspace.id, expect.objectContaining({
      dataSetRequest: expectedDatasetRequest,
      kernelType: KernelTypeEnum.R
    }));

    wrapper.find('[data-test-id="code-preview-button"]').simulate('click');
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('iframe').exists()).toBeFalsy();
  });
});
