import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {mount} from 'enzyme';
import {DataSet, DataSetApi, KernelTypeEnum, PrePackagedConceptSetEnum, WorkspacesApi} from 'generated/fetch/api';
import * as React from 'react';
import {waitOneTickAndUpdate} from 'testing/react-test-helpers';
import {DataSetApiStub} from 'testing/stubs/data-set-api-stub';
import {WorkspacesApiStub} from 'testing/stubs/workspaces-api-stub';
import {ExportDataSetModal} from './export-data-set-modal';

const workspaceNamespace = 'workspaceNamespace';
const workspaceFirecloudName = 'workspaceId';
const dataSet: DataSet = {
  name: 'hello world',
  description: 'hi',
  conceptSets: [],
  cohorts: [],
  domainValuePairs: [],
  includesAllParticipants: false,
  prePackagedConceptSet: Array.of(PrePackagedConceptSetEnum.SURVEY, PrePackagedConceptSetEnum.FITBITACTIVITY)
};

const createExportDataSetModal = () => {
  return <ExportDataSetModal
    closeFunction={() => {}}
    workspaceNamespace={workspaceNamespace}
    workspaceFirecloudName={workspaceFirecloudName}
    dataSet={dataSet}
  />;
}


describe('ExportDataSetModal', () => {
  beforeEach(() => {
    registerApiClient(WorkspacesApi, new WorkspacesApiStub());
    registerApiClient(DataSetApi, new DataSetApiStub());
  });


  it('should render', () => {
    const wrapper = mount(createExportDataSetModal());
    expect(wrapper.exists()).toBeTruthy();
  });

  it ('should change/update notebook format Type from default Python', async() => {
    const wrapper = mount(createExportDataSetModal());
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.instance().state['kernelType']).toBe(KernelTypeEnum.Python);
    wrapper.find('[data-test-id="kernel-type-r"]').first().simulate('click');
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.instance().state['kernelType']).toBe(KernelTypeEnum.R);
  });
})
