import '@testing-library/jest-dom';

import * as React from 'react';
import { MemoryRouter } from 'react-router';

import { PrePackagedConceptSetEnum, WorkspaceResource } from 'generated/fetch';

import { fireEvent, render } from '@testing-library/react';
import { serverConfigStore } from 'app/utils/stores';

import { exampleCohortStubs } from 'testing/stubs/cohorts-api-stub';
import { stubDataSet } from 'testing/stubs/data-set-api-stub';
import { stubResource } from 'testing/stubs/resources-stub';
import { workspaceDataStub } from 'testing/stubs/workspaces';

import { ResourceActionMenu } from './resource-action-menu';

describe(ResourceActionMenu.name, () => {
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

    const { getByTestId } = render(
      <MemoryRouter>
        <ResourceActionMenu
          resource={testCohort}
          workspace={workspaceDataStub}
          existingNameList={[]}
          onUpdate={async () => {}}
        />
      </MemoryRouter>
    );
    expect(getByTestId('resource-card-menu')).toBeInTheDocument();
  });

  it('renders a dataset menu', async () => {
    const testDataSet = {
      ...stubResource,
      dataSet: {
        ...stubDataSet(),
        prePackagedConceptSet: [PrePackagedConceptSetEnum.FITBIT],
      },
    } as WorkspaceResource;

    const { getByTestId } = render(
      <MemoryRouter>
        <ResourceActionMenu
          resource={testDataSet}
          workspace={workspaceDataStub}
          existingNameList={[]}
          onUpdate={async () => {}}
        />
      </MemoryRouter>
    );
    fireEvent.click(getByTestId('resource-card-menu'));
    expect(getByTestId('resource-card-menu')).toHaveTextContent(
      'Export to Notebook'
    );
    expect(getByTestId('resource-card-menu')).not.toHaveTextContent(
      'Extract VCF Files'
    );
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

    const { getByTestId } = render(
      <MemoryRouter>
        <ResourceActionMenu
          resource={testDataSet}
          workspace={workspaceDataStub}
          existingNameList={[]}
          onUpdate={async () => {}}
        />
      </MemoryRouter>
    );
    fireEvent.click(getByTestId('resource-card-menu'));
    expect(getByTestId('resource-card-menu')).toHaveTextContent(
      'Export to Notebook'
    );
    expect(getByTestId('resource-card-menu')).toHaveTextContent(
      'Extract VCF Files'
    );
  });
});
