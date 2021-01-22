import {mount} from 'enzyme';
import * as React from 'react';

import {dataSetApi, registerApiClient} from 'app/services/swagger-fetch-clients';
import {
  DataSetApi,
  DataSetRequest,
  KernelTypeEnum,
  PrePackagedConceptSetEnum,
  WorkspacesApi} from 'generated/fetch';
import {waitOneTickAndUpdate} from 'testing/react-test-helpers';
import {DataSetApiStub} from 'testing/stubs/data-set-api-stub';
import {WorkspacesApiStub} from 'testing/stubs/workspaces-api-stub';
import {DataSetExportRequest} from 'generated/fetch';
import {NewDataSetModal} from './new-dataset-modal';
import GenomicsAnalysisToolEnum = DataSetExportRequest.GenomicsAnalysisToolEnum;
import GenomicsDataTypeEnum = DataSetExportRequest.GenomicsDataTypeEnum;
import {ExportDataSet} from "./export-data-set";

const prePackagedConceptSet = Array.of(PrePackagedConceptSetEnum.NONE);
const workspaceNamespace = 'workspaceNamespace';
const workspaceId = 'workspaceId';

let dataSet;

const createExportDataSetModal = () => {
  return <ExportDataSet newNotebook={()=>{}}
                        updateNotebookName={()=>{}}
                        workspaceNamespace={workspaceNamespace}
                        workspaceFirecloudName={workspaceId}
                        dataSetRequest={dataSet}
  />;
};

describe('ExportDataSet', () => {
  beforeEach(() => {
    window.open = jest.fn();
    dataSet = undefined;
    registerApiClient(WorkspacesApi, new WorkspacesApiStub());
    registerApiClient(DataSetApi, new DataSetApiStub());
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('should render', () => {
    const wrapper = mount(createExportDataSetModal());
    expect(wrapper.exists()).toBeTruthy();
  });

  it('should only display code when button pressed', async() => {
    const wrapper = mount(createExportDataSetModal());
    expect(wrapper.find('[data-test-id="code-text-box"]').exists()).toBeFalsy();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="code-preview-button"]')
        .first().text()).toEqual('See Code Preview');
    wrapper.find('[data-test-id="code-preview-button"]').first().simulate('click');
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="code-preview-button"]')
        .first().text()).toEqual('Hide Preview');
  });

});
