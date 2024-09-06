import '@testing-library/jest-dom';

import * as React from 'react';
import { Router } from 'react-router';
import { createMemoryHistory } from 'history';

import { CohortBuilderApi, CohortsApi } from 'generated/fetch';

import { render, screen, waitFor } from '@testing-library/react';
import {
  cohortsApi,
  registerApiClient,
} from 'app/services/swagger-fetch-clients';
import { currentWorkspaceStore } from 'app/utils/navigation';
import { cdrVersionStore, serverConfigStore } from 'app/utils/stores';

import defaultServerConfig from 'testing/default-server-config';
import { cdrVersionTiersResponse } from 'testing/stubs/cdr-versions-api-stub';
import { CohortBuilderServiceStub } from 'testing/stubs/cohort-builder-service-stub';
import { CohortsApiStub } from 'testing/stubs/cohorts-api-stub';
import { workspaceDataStub } from 'testing/stubs/workspaces';

import { CohortPage } from './cohort-page';

describe('CohortPage', () => {
  let history;

  beforeEach(() => {
    currentWorkspaceStore.next(workspaceDataStub);
    cdrVersionStore.set(cdrVersionTiersResponse);
    serverConfigStore.set({ config: defaultServerConfig });
    registerApiClient(CohortBuilderApi, new CohortBuilderServiceStub());
    registerApiClient(CohortsApi, new CohortsApiStub());
    history = createMemoryHistory();
  });

  const component = () => {
    return render(
      <Router history={history}>
        <CohortPage
          setCohortChanged={() => {}}
          setShowWarningModal={() => {}}
          setUpdatingCohort={() => {}}
          hideSpinner={() => {}}
          showSpinner={() => {}}
        />
      </Router>
    );
  };

  it('should render', async () => {
    component();
    expect(await screen.findByText(/group 1/i)).toBeInTheDocument();
  });

  it('should render one search group for each includes/excludes item', async () => {
    const mockGetCohort = jest.spyOn(cohortsApi(), 'getCohort');
    const { namespace, terraName } = workspaceDataStub;
    component();
    await waitFor(() => expect(mockGetCohort).toHaveBeenCalledTimes(0));
    expect(screen.queryAllByTestId('includes-search-group').length).toBe(0);
    expect(screen.queryAllByTestId('excludes-search-group').length).toBe(0);

    // Call cohort with 2 includes groups
    history.push('?cohortId=1');
    await waitFor(() =>
      expect(mockGetCohort).toHaveBeenCalledWith(namespace, terraName, 1)
    );
    expect(screen.getAllByTestId('includes-search-group').length).toBe(2);
    expect(screen.queryAllByTestId('excludes-search-group').length).toBe(0);

    // Call cohort with 2 includes groups and one excludes group
    history.push('?cohortId=2');
    await waitFor(() =>
      expect(mockGetCohort).toHaveBeenCalledWith(namespace, terraName, 2)
    );
    expect(screen.getAllByTestId('includes-search-group').length).toBe(2);
    expect(screen.getAllByTestId('excludes-search-group').length).toBe(1);
  });
});
