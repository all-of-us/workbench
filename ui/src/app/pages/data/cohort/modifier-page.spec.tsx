import * as React from 'react';
import { MemoryRouter, Route } from 'react-router-dom';
import { mount } from 'enzyme';

import {
  CohortBuilderApi,
  Domain,
  ModifierType,
  WorkspacesApi,
} from 'generated/fetch';

import { registerApiClient } from 'app/services/swagger-fetch-clients';
import {
  currentCohortSearchContextStore,
  currentWorkspaceStore,
} from 'app/utils/navigation';
import { cdrVersionStore, serverConfigStore } from 'app/utils/stores';

import { waitOneTickAndUpdate } from 'testing/react-test-helpers';
import { cdrVersionTiersResponse } from 'testing/stubs/cdr-versions-api-stub';
import { CohortBuilderServiceStub } from 'testing/stubs/cohort-builder-service-stub';
import { workspaceDataStub } from 'testing/stubs/workspaces';
import { WorkspacesApiStub } from 'testing/stubs/workspaces-api-stub';

import { ModifierPage } from './modifier-page';

describe('ModifierPage', () => {
  beforeEach(() => {
    registerApiClient(WorkspacesApi, new WorkspacesApiStub());
    registerApiClient(CohortBuilderApi, new CohortBuilderServiceStub());

    currentCohortSearchContextStore.next({
      domain: Domain.CONDITION,
      item: { modifiers: [] },
    });
    currentWorkspaceStore.next(workspaceDataStub);
    serverConfigStore.set({
      config: {
        enableEventDateModifier: false,
        gsuiteDomain: 'fake-research-aou.org',
        projectId: 'aaa',
        publicApiKeyForErrorReports: 'aaa',
        enableEraCommons: true,
      },
    });
    cdrVersionStore.set(cdrVersionTiersResponse);
  });

  const component = () => {
    return mount(
      <MemoryRouter
        initialEntries={[
          `/workspaces/${workspaceDataStub.namespace}/${workspaceDataStub.id}/data/cohorts/build`,
        ]}
      >
        <Route exact path='/workspaces/:ns/:wsid/data/cohorts/build'>
          <ModifierPage closeModifiers={() => {}} selections={[]} />
        </Route>
      </MemoryRouter>
    );
  };

  it('should render', () => {
    const wrapper = component();
    expect(wrapper.exists()).toBeTruthy();
  });

  it('should display Only Age Event and CATI modifiers for SURVEY', async () => {
    currentCohortSearchContextStore.next({
      domain: Domain.SURVEY,
      item: { modifiers: [] },
    });
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.exists()).toBeTruthy();
    expect(
      wrapper.find('[data-test-id="' + ModifierType.AGE_AT_EVENT + '"]').length
    ).toBeGreaterThan(0);
    expect(
      wrapper.find('[data-test-id="' + ModifierType.NUM_OF_OCCURRENCES + '"]')
        .length
    ).toBe(0);
    expect(
      wrapper.find('[data-test-id="' + ModifierType.ENCOUNTERS + '"]').length
    ).toBe(0);
    expect(
      wrapper.find('[data-test-id="' + ModifierType.EVENT_DATE + '"]').length
    ).toBe(0);
  });
});
