import '@testing-library/jest-dom';

import * as React from 'react';

import { WorkspaceResource } from 'generated/fetch';

import { screen } from '@testing-library/react';

import { renderWithRouter } from 'testing/react-test-helpers';
import { stubResource } from 'testing/stubs/resources-stub';
import { workspaceDataStub } from 'testing/stubs/workspaces';

import { ResourceList } from './resource-list';

export const RESOURCE_TYPE_COLUMN_NUMBER = 1;
export const NAME_COLUMN_NUMBER = 2;
export const MODIFIED_DATE_COLUMN_NUMBER = 3;
export const MODIFIED_BY_COLUMN_NUMBER = 4;

export const resourceTable = (wrapper) =>
  wrapper.find('[data-test-id="resource-list"]').find('tbody');
export const resourceTableRows = (wrapper) => resourceTable(wrapper).find('tr');
export const resourceTableColumns = (wrapper) =>
  resourceTable(wrapper).find('td');

const COHORT_NAME = 'My Cohort';
const COHORT: WorkspaceResource = {
  ...stubResource,
  cohort: {
    name: COHORT_NAME,
    criteria: 'something',
    type: 'something else',
  },
};

describe(ResourceList.name, () => {
  it('should render when there are no resources', async () => {
    renderWithRouter(<ResourceList workspaceResources={[]} />);
    await screen.findByTestId('resources-table');

    // no resources are rendered
    expect(screen.queryByText(/item type/i)).not.toBeInTheDocument();
    expect(
      screen.queryByPlaceholderText(/search name/i)
    ).not.toBeInTheDocument();
  });

  it('should render a cohort resource', async () => {
    renderWithRouter(
      <ResourceList
        workspaces={[workspaceDataStub]}
        workspaceResources={[COHORT]}
      />
    );

    await screen.findByTestId('resources-table');
    expect(screen.queryByText(/item type/i)).toBeInTheDocument();
    expect(screen.queryByPlaceholderText(/search name/i)).toBeInTheDocument();
    expect(screen.getByText('Cohort')).toBeInTheDocument();
    expect(screen.getByText(COHORT_NAME)).toBeInTheDocument();
  });

  it('should not render a resource when its workspace is not available', async () => {
    renderWithRouter(
      <ResourceList workspaces={[]} workspaceResources={[COHORT]} />
    );
    await screen.findByTestId('resources-table');

    // no resources are rendered
    expect(screen.queryByText(/item type/i)).not.toBeInTheDocument();
    expect(
      screen.queryByPlaceholderText(/search name/i)
    ).not.toBeInTheDocument();
  });
});
