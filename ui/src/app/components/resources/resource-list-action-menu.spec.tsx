import '@testing-library/jest-dom';

import * as React from 'react';

import { PrePackagedConceptSetEnum, WorkspaceResource } from 'generated/fetch';

import { render } from '@testing-library/react';
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

  it('does not render a menu for an invalid resource', () => {
    // stubResource is only a base type for valid resources.
    // To be valid, it needs exactly one of these to be defined:
    // cohort, cohortReview, conceptSet, dataSet, notebook
    const invalidResource: WorkspaceResource = stubResource;
    const menu = render(
      <ResourceListActionMenu
        resource={invalidResource}
        workspace={workspaceDataStub}
        existingNameList={[]}
        onUpdate={async () => {}}
      />
    );
    expect(menu.container).toBeEmptyDOMElement();
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
    // can't extract without WGS
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
    // WGS allows extraction
    expect(queryByText('Extract VCF Files')).toBeInTheDocument();
  });
});
