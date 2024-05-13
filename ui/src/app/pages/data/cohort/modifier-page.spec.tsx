import '@testing-library/jest-dom';

import * as React from 'react';
import { MemoryRouter, Route } from 'react-router-dom';

import {
  CohortBuilderApi,
  Domain,
  ModifierType,
  WorkspacesApi,
} from 'generated/fetch';

import { render, screen, waitFor } from '@testing-library/react';
import { dataTabPath } from 'app/routing/utils';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
import {
  currentCohortSearchContextStore,
  currentWorkspaceStore,
} from 'app/utils/navigation';
import { cdrVersionStore, serverConfigStore } from 'app/utils/stores';

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
    return render(
      <MemoryRouter
        initialEntries={[
          `${dataTabPath(
            workspaceDataStub.namespace,
            workspaceDataStub.id
          )}/cohorts/build`,
        ]}
      >
        <Route exact path='/workspaces/:ns/:wsid/data/cohorts/build'>
          <ModifierPage closeModifiers={() => {}} selections={[]} />
        </Route>
      </MemoryRouter>
    );
  };

  it('should render', async () => {
    component();
    await screen.findByRole('heading', {
      name: /apply optional modifiers/i,
    });
  });

  it('should display Only Age Event and CATI modifiers for SURVEY', async () => {
    currentCohortSearchContextStore.next({
      domain: Domain.SURVEY,
      item: { modifiers: [] },
    });
    component();

    await waitFor(() => {
      expect(screen.queryByLabelText('Please Wait')).not.toBeInTheDocument();
    });
    expect(screen.getByTestId(ModifierType.AGE_AT_EVENT)).toBeInTheDocument();
    expect(
      screen.queryByTestId(ModifierType.NUM_OF_OCCURRENCES)
    ).not.toBeInTheDocument();
    expect(
      screen.queryByTestId(ModifierType.ENCOUNTERS)
    ).not.toBeInTheDocument();
    expect(
      screen.queryByTestId(ModifierType.EVENT_DATE)
    ).not.toBeInTheDocument();
  });
});
