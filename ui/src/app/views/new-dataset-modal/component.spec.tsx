import {mount} from 'enzyme';
import * as React from 'react';

import {NewDataSetModal} from './component';
import {DataSetApi, WorkspacesApi} from 'generated/fetch';
import {DataSetApiStub} from 'testing/stubs/data-set-api-stub';
import {WorkspacesApiStub} from 'testing/stubs/workspaces-api-stub';
import {dataSetApi, registerApiClient} from 'app/services/swagger-fetch-clients';
import {waitOneTickAndUpdate} from 'testing/react-test-helpers';

const workspaceNamespace = 'workspaceNamespace';
const workspaceId = 'workspaceId';

const createNewDataSetModal = () => {
  return <NewDataSetModal
    closeFunction={() => {}}
    includesAllParticipants={false}
    selectedConceptSetIds={[]}
    selectedCohortIds={[]}
    selectedValues={[]}
    workspaceNamespace={workspaceNamespace}
    workspaceId={workspaceId}
  />;
}

describe('NewDataSetModal', () => {
  beforeEach(() => {
    registerApiClient(WorkspacesApi, new WorkspacesApiStub());
    registerApiClient(DataSetApi, new DataSetApiStub());
  });


  it('should render', () => {
    const wrapper = mount(createNewDataSetModal());
    expect(wrapper.exists()).toBeTruthy();
  });

  it('should only display code when button pressed', async() => {
    const wrapper = mount(createNewDataSetModal());
    expect(wrapper.find('[data-test-id="code-text-box"]').exists()).toBeFalsy();
    wrapper.find('[data-test-id="export-to-notebook"]').first().simulate('change');
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="code-preview-button"]')
      .first().text()).toEqual('See Code Preview');
    wrapper.find('[data-test-id="code-preview-button"]').first().simulate('click');
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="code-preview-button"]')
      .first().text()).toEqual('Hide Preview');
  });

  it('should not allow submission if no name specified', async() => {
    const wrapper = mount(createNewDataSetModal());
    const createSpy = jest.spyOn(dataSetApi(), 'createDataSet');
    const exportSpy = jest.spyOn(dataSetApi(), 'exportToNotebook');

    wrapper.find('[data-test-id="save-data-set"]').first().simulate('click');
    await waitOneTickAndUpdate(wrapper);
    expect(createSpy).not.toHaveBeenCalled();
    expect(exportSpy).not.toHaveBeenCalled();
  });

  it('should not export if export not clicked', async() => {
    const wrapper = mount(createNewDataSetModal());
    const createSpy = jest.spyOn(dataSetApi(), 'createDataSet');
    const exportSpy = jest.spyOn(dataSetApi(), 'exportToNotebook');
    const nameStub = 'Data Set Name';

    wrapper.find('[data-test-id="data-set-name-input"]')
      .first().simulate('change', {target: {value: nameStub}});
    await waitOneTickAndUpdate(wrapper);
    wrapper.find('[data-test-id="save-data-set"]').first().simulate('click');
    await waitOneTickAndUpdate(wrapper);
    expect(createSpy).toHaveBeenCalledWith(workspaceNamespace, workspaceId, {
      name: nameStub,
      includesAllParticipants: false,
      description: '',
      conceptSetIds: [],
      cohortIds: [],
      values: []
    });
    expect(exportSpy).not.toHaveBeenCalled();
  });

  it('should not submit if export checked but no name or selected notebook', async() => {
    const wrapper = mount(createNewDataSetModal());
    const createSpy = jest.spyOn(dataSetApi(), 'createDataSet');
    const exportSpy = jest.spyOn(dataSetApi(), 'exportToNotebook');
    const nameStub = 'Data Set Name';

    wrapper.find('[data-test-id="data-set-name-input"]')
      .first().simulate('change', {target: {value: nameStub}});
    await waitOneTickAndUpdate(wrapper);
    wrapper.find('[data-test-id="export-to-notebook"]').first().simulate('change');
    await waitOneTickAndUpdate(wrapper);
    wrapper.find('[data-test-id="save-data-set"]').first().simulate('click');
    await waitOneTickAndUpdate(wrapper);
    expect(createSpy).not.toHaveBeenCalled();
    expect(exportSpy).not.toHaveBeenCalled();
  });

  it('should submit if export checked and name specified', async() => {
    const wrapper = mount(createNewDataSetModal());
    const createSpy = jest.spyOn(dataSetApi(), 'createDataSet');
    const exportSpy = jest.spyOn(dataSetApi(), 'exportToNotebook');
    const nameStub = 'Data Set Name';
    const notebookNameStub = 'Notebook Name';
    const dataSetRequestStub = {
      name: nameStub,
      includesAllParticipants: false,
      description: '',
      conceptSetIds: [],
      cohortIds: [],
      values: []
    };

    wrapper.find('[data-test-id="data-set-name-input"]')
      .first().simulate('change', {target: {value: nameStub}});
    await waitOneTickAndUpdate(wrapper);
    wrapper.find('[data-test-id="export-to-notebook"]').first().simulate('change');
    await waitOneTickAndUpdate(wrapper);
    wrapper.find('[data-test-id="notebook-name-input"]')
      .first().simulate('change', {target: {value: notebookNameStub}});
    await waitOneTickAndUpdate(wrapper);
    wrapper.find('[data-test-id="save-data-set"]').first().simulate('click');
    await waitOneTickAndUpdate(wrapper);
    expect(createSpy).toHaveBeenCalled();
    expect(exportSpy).toHaveBeenCalledWith(workspaceNamespace, workspaceId, {
      dataSetRequest: dataSetRequestStub,
      newNotebook: true,
      notebookName: notebookNameStub
    });
  });
});
