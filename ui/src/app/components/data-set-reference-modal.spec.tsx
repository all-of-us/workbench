import '@testing-library/jest-dom';

import { WorkspaceResource } from 'generated/fetch';
import { DataSetApi } from 'generated/fetch';

import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {
  dataSetApi,
  registerApiClient,
} from 'app/services/swagger-fetch-clients';

import { exampleCohortStubs } from 'testing/stubs/cohorts-api-stub';
import { DataSetApiStub } from 'testing/stubs/data-set-api-stub';
import { stubResource } from 'testing/stubs/resources-stub';

import { DataSetReferenceModal } from './data-set-reference-modal';

const testCohort = {
  ...stubResource,
  cohort: exampleCohortStubs[0],
} as WorkspaceResource;

const onCancelFn = jest.fn();
const deleteResourceFn = jest.fn();

const renderModal = () => {
  return render(
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
    renderModal();
    expect(screen.getByText('WARNING')).toBeInTheDocument();
  });

  it('should cancel deletion', async () => {
    renderModal();

    const cancelButton = screen.getByText('Cancel');
    userEvent.click(cancelButton);

    await waitFor(() => {
      expect(onCancelFn).toHaveBeenCalled();
      expect(deleteResourceFn).not.toHaveBeenCalled();
    });
  });

  it('should markDirty and delete', async () => {
    registerApiClient(DataSetApi, new DataSetApiStub());
    const markDirty = jest.spyOn(dataSetApi(), 'markDirty');
    renderModal();

    const deleteButton = screen.getByText('YES, DELETE');
    userEvent.click(deleteButton);

    await waitFor(() => {
      expect(markDirty).toHaveBeenCalled();
      expect(deleteResourceFn).toHaveBeenCalled();

      expect(onCancelFn).not.toHaveBeenCalled();
    });
  });
});
