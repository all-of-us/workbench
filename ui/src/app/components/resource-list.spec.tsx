import * as React from 'react';
import { MemoryRouter } from 'react-router';
import { mount } from 'enzyme';

import { WorkspaceResource } from 'generated/fetch';

import { waitOneTickAndUpdate } from 'testing/react-test-helpers';
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

describe('ResourceList', () => {
  it('should render when there are no resources', async () => {
    const wrapper = mount(
      <MemoryRouter>
        <ResourceList workspaceResources={[]} />
      </MemoryRouter>
    );
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.exists()).toBeTruthy();

    // no resources are rendered
    expect(
      resourceTableColumns(wrapper).at(RESOURCE_TYPE_COLUMN_NUMBER).exists()
    ).toBeFalsy();
    expect(
      resourceTableColumns(wrapper).at(NAME_COLUMN_NUMBER).exists()
    ).toBeFalsy();
  });

  it('should render a cohort resource', async () => {
    const wrapper = mount(
      <MemoryRouter>
        <ResourceList
          workspaces={[workspaceDataStub]}
          workspaceResources={[COHORT]}
        />
      </MemoryRouter>
    );
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.exists()).toBeTruthy();

    expect(
      resourceTableColumns(wrapper)
        .at(RESOURCE_TYPE_COLUMN_NUMBER)
        .find('div')
        .first()
        .text()
    ).toBe('Cohort');
    expect(
      resourceTableColumns(wrapper)
        .at(NAME_COLUMN_NUMBER)
        .find('div')
        .first()
        .text()
    ).toBe(COHORT_NAME);
  });

  it('should not render a resource when its workspace is not available', async () => {
    const wrapper = mount(
      <MemoryRouter>
        <ResourceList workspaces={[]} workspaceResources={[COHORT]} />
      </MemoryRouter>
    );
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.exists()).toBeTruthy();

    // the resource is not rendered, because its workspace is not available
    expect(
      resourceTableColumns(wrapper).at(RESOURCE_TYPE_COLUMN_NUMBER).exists()
    ).toBeFalsy();
    expect(
      resourceTableColumns(wrapper).at(NAME_COLUMN_NUMBER).exists()
    ).toBeFalsy();
  });
});
