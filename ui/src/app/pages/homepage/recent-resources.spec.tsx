import * as React from 'react';
import { MemoryRouter } from 'react-router-dom';
import { mount } from 'enzyme';

import { UserMetricsApi, WorkspaceAccessLevel } from 'generated/fetch';

import {
  MODIFIED_DATE_COLUMN_NUMBER,
  NAME_COLUMN_NUMBER,
  RESOURCE_TYPE_COLUMN_NUMBER,
  resourceTableColumns,
} from 'app/components/resource-list.spec';
import { RecentResources } from 'app/pages/homepage/recent-resources';
import { registerApiClient } from 'app/services/swagger-fetch-clients';

import { waitOneTickAndUpdate } from 'testing/react-test-helpers';
import {
  UserMetricsApiStub,
  userMetricsApiStubResources,
} from 'testing/stubs/user-metrics-api-stub';
import { workspaceStubs } from 'testing/stubs/workspaces';

describe('RecentResourcesComponent', () => {
  beforeEach(() => {
    registerApiClient(UserMetricsApi, new UserMetricsApiStub());
  });

  it('should render resources in a workspace', async () => {
    const wrapper = mount(
      <MemoryRouter>
        <RecentResources
          workspaces={[
            {
              workspace: workspaceStubs[0],
              accessLevel: WorkspaceAccessLevel.OWNER,
            },
          ]}
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
    ).toBe(userMetricsApiStubResources[0].cohort.name);
  });

  it('should not render resources when their workspace is not available', async () => {
    const wrapper = mount(
      <MemoryRouter>
        <RecentResources workspaces={[]} />
      </MemoryRouter>
    );
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.exists()).toBeTruthy();

    expect(
      resourceTableColumns(wrapper).at(RESOURCE_TYPE_COLUMN_NUMBER).exists()
    ).toBeFalsy();
    expect(
      resourceTableColumns(wrapper).at(NAME_COLUMN_NUMBER).exists()
    ).toBeFalsy();
    expect(
      resourceTableColumns(wrapper).at(MODIFIED_DATE_COLUMN_NUMBER).exists()
    ).toBeFalsy();
  });
});
