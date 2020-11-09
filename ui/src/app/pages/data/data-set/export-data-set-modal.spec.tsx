import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {mount} from 'enzyme';
import {Dataset, DatasetApi, PrePackagedConceptSetEnum, WorkspacesApi} from 'generated/fetch/api';
import * as React from 'react';
import {DatasetApiStub} from 'testing/stubs/data-set-api-stub';
import {WorkspacesApiStub} from 'testing/stubs/workspaces-api-stub';
import {ExportDatasetModal} from './export-data-set-modal';

const workspaceNamespace = 'workspaceNamespace';
const workspaceFirecloudName = 'workspaceId';
const dataset: Dataset = {
  name: 'hello world',
  description: 'hi',
  conceptSets: [],
  cohorts: [],
  domainValuePairs: [],
  includesAllParticipants: false,
  prePackagedConceptSet: PrePackagedConceptSetEnum.SURVEY
};

const createExportDatasetModal = () => {
  return <ExportDatasetModal
    closeFunction={() => {}}
    workspaceNamespace={workspaceNamespace}
    workspaceFirecloudName={workspaceFirecloudName}
    dataset={dataset}
  />;
}


describe('ExportDatasetModal', () => {
  beforeEach(() => {
    registerApiClient(WorkspacesApi, new WorkspacesApiStub());
    registerApiClient(DatasetApi, new DatasetApiStub());
  });


  it('should render', () => {
    const wrapper = mount(createExportDatasetModal());
    expect(wrapper.exists()).toBeTruthy();
  });
})
