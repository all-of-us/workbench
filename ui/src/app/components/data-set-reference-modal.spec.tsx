import * as React from 'react';
import { mount } from 'enzyme';

import { WorkspaceResource } from 'generated/fetch';
import { DataSetApi } from 'generated/fetch';

import {
  dataSetApi,
  registerApiClient,
} from 'app/services/swagger-fetch-clients';

import { waitOneTickAndUpdate } from 'testing/react-test-helpers';
import { exampleCohortStubs } from 'testing/stubs/cohorts-api-stub';
import { DataSetApiStub } from 'testing/stubs/data-set-api-stub';
import { stubResource } from 'testing/stubs/resources-stub';

import { Button } from './buttons';
import { DataSetReferenceModal } from './data-set-reference-modal';

const testCohort = {
  ...stubResource,
  cohort: exampleCohortStubs[0],
} as WorkspaceResource;

const onCancelFn = jest.fn();
const deleteResourceFn = jest.fn();

const mountModal = () => {
  return mount(
    <DataSetReferenceModal
      referencedResource={testCohort}
      dataSets=''
      onCancel={onCancelFn}
      deleteResource={deleteResourceFn}
    />
  );
};

describe('DataSetReferenceModal', () => {
  it('should render', async () => {
    const wrapper = mountModal();
    expect(wrapper.exists()).toBeTruthy();
  });

  it('should cancel deletion', async () => {
    const wrapper = mountModal();

    const cancelButton = wrapper.find(Button).find({ type: 'secondary' });
    cancelButton.simulate('click');

    expect(onCancelFn).toHaveBeenCalled();
    expect(deleteResourceFn).not.toHaveBeenCalled();
  });

  it('should markDirty and delete', async () => {
    registerApiClient(DataSetApi, new DataSetApiStub());
    const markDirty = jest.spyOn(dataSetApi(), 'markDirty');
    const wrapper = mountModal();

    const deleteButton = wrapper.find(Button).find({ type: 'primary' });
    deleteButton.simulate('click');
    await waitOneTickAndUpdate(wrapper);

    expect(markDirty).toHaveBeenCalled();
    expect(deleteResourceFn).toHaveBeenCalled();

    expect(onCancelFn).not.toHaveBeenCalled();
  });
});
