import '@testing-library/jest-dom';

import * as React from 'react';

import { PrePackagedConceptSetEnum, WorkspaceResource } from 'generated/fetch';

import { fireEvent } from '@testing-library/react';
import { serverConfigStore } from 'app/utils/stores';

import { renderWithRouter } from 'testing/react-test-helpers';
import { exampleCohortStubs } from 'testing/stubs/cohorts-api-stub';
import { stubDataSet } from 'testing/stubs/data-set-api-stub';
import { stubResource } from 'testing/stubs/resources-stub';
import { workspaceDataStub } from 'testing/stubs/workspaces';

import { ResourceListActionMenu } from './resource-list-action-menu';

describe(ResourceListActionMenu.name, () => {
  beforeEach(() => {
    serverConfigStore.set({
      config: { gsuiteDomain: '' },
    });
  });

  it('renders a Cohort menu', async () => {
    const testCohort = {
      ...stubResource,
      cohort: exampleCohortStubs[0],
    } as WorkspaceResource;

    const { getByTitle } = renderWithRouter(
      <ResourceListActionMenu
        resource={testCohort}
        workspace={workspaceDataStub}
        existingNameList={[]}
        onUpdate={async () => {}}
      />
    );
    expect(getByTitle('Cohort Action Menu')).toBeInTheDocument();
  });

  it('renders a dataset menu, without WGS', async () => {
    const testDataSet = {
      ...stubResource,
      dataSet: {
        ...stubDataSet(),
        prePackagedConceptSet: [PrePackagedConceptSetEnum.FITBIT],
      },
    } as WorkspaceResource;

    const { getByTitle, getByText, queryByText } = renderWithRouter(
      <ResourceListActionMenu
        resource={testDataSet}
        workspace={workspaceDataStub}
        existingNameList={[]}
        onUpdate={async () => {}}
      />
    );
    fireEvent.click(getByTitle('Dataset Action Menu'));
    expect(getByText('Export to Notebook')).toBeInTheDocument();
    expect(queryByText('Extract VCF Files')).not.toBeInTheDocument();
  });

  it('renders a dataset menu, with WGS', async () => {
    const testDataSet = {
      ...stubResource,
      dataSet: {
        ...stubDataSet(),
        prePackagedConceptSet: [
          PrePackagedConceptSetEnum.PERSON,
          PrePackagedConceptSetEnum.WHOLE_GENOME,
        ],
      },
    } as WorkspaceResource;

    const { getByTitle, getByText, queryByText } = renderWithRouter(
      <ResourceListActionMenu
        resource={testDataSet}
        workspace={workspaceDataStub}
        existingNameList={[]}
        onUpdate={async () => {}}
      />
    );
    fireEvent.click(getByTitle('Dataset Action Menu'));
    expect(getByText('Export to Notebook')).toBeInTheDocument();
    expect(queryByText('Extract VCF Files')).toBeInTheDocument();
  });
});
