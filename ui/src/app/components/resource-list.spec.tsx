import * as React from 'react';
import { MemoryRouter } from 'react-router';
import { mount } from 'enzyme';

import { WorkspaceResource } from 'generated/fetch';

import { waitOneTickAndUpdate } from 'testing/react-test-helpers';
import { workspaceDataStub } from 'testing/stubs/workspaces';

import { ResourceList } from './resource-list';

const RESOURCE_TYPE_COLUMN_NUMBER = 1;
const NAME_COLUMN_NUMBER = 2;

const COHORT_NAME = 'My Cohort';
const COHORT: Partial<WorkspaceResource> = {
  workspaceNamespace: workspaceDataStub.namespace,
  workspaceFirecloudName: workspaceDataStub.id,
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

    const Columns = wrapper
      .find('[data-test-id="resource-list"]')
      .find('tbody')
      .find('td');

    // no resources are rendered
    expect(Columns.at(RESOURCE_TYPE_COLUMN_NUMBER).exists()).toBeFalsy();
    expect(Columns.at(NAME_COLUMN_NUMBER).exists()).toBeFalsy();
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

    const Columns = wrapper
      .find('[data-test-id="resource-list"]')
      .find('tbody')
      .find('td');

    expect(Columns.at(RESOURCE_TYPE_COLUMN_NUMBER).text()).toBe('Cohort');
    expect(Columns.at(NAME_COLUMN_NUMBER).text()).toBe(COHORT_NAME);
  });

  it("should render when a resource's workspace is not available", async () => {
    const wrapper = mount(
      <MemoryRouter>
        <ResourceList workspaces={[]} workspaceResources={[COHORT]} />
      </MemoryRouter>
    );
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.exists()).toBeTruthy();

    const Columns = wrapper
      .find('[data-test-id="resource-list"]')
      .find('tbody')
      .find('td');

    // the resource is not rendered, because its workspace is not available
    expect(Columns.at(RESOURCE_TYPE_COLUMN_NUMBER).exists()).toBeFalsy();
    expect(Columns.at(NAME_COLUMN_NUMBER).exists()).toBeFalsy();
  });
});
