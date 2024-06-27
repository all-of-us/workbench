import '@testing-library/jest-dom';

import * as React from 'react';

import { CohortBuilderApi, CohortsApi } from 'generated/fetch';

import { screen, waitFor } from '@testing-library/react';
import { dataTabPath } from 'app/routing/utils';
import {
  cohortsApi,
  registerApiClient,
} from 'app/services/swagger-fetch-clients';
import { currentWorkspaceStore } from 'app/utils/navigation';
import { cdrVersionStore, serverConfigStore } from 'app/utils/stores';

import defaultServerConfig from 'testing/default-server-config';
import { renderWithRouterAndPath } from 'testing/react-test-helpers';
import { cdrVersionTiersResponse } from 'testing/stubs/cdr-versions-api-stub';
import { CohortBuilderServiceStub } from 'testing/stubs/cohort-builder-service-stub';
import { CohortsApiStub } from 'testing/stubs/cohorts-api-stub';
import { workspaceDataStub } from 'testing/stubs/workspaces';

import { CohortPage } from './cohort-page';

describe(CohortPage.name, () => {
  beforeEach(() => {
    currentWorkspaceStore.next(workspaceDataStub);
    cdrVersionStore.set(cdrVersionTiersResponse);
    serverConfigStore.set({ config: defaultServerConfig });
    registerApiClient(CohortBuilderApi, new CohortBuilderServiceStub());
    registerApiClient(CohortsApi, new CohortsApiStub());
  });

  const component = (queryParams?: string) => {
    const path =
      dataTabPath('foo', 'bar') +
      'cohorts/build' +
      (queryParams ? `?${queryParams}` : '');
    return renderWithRouterAndPath(
      path,
      <CohortPage
        setCohortChanged={() => {}}
        setShowWarningModal={() => {}}
        setUpdatingCohort={() => {}}
        hideSpinner={() => {}}
        showSpinner={() => {}}
      />
    );
  };

  it('should render', async () => {
    component();
    expect(await screen.findByText(/group 1/i)).toBeInTheDocument();
  });

  it('should render one search group for each includes/excludes item', async () => {
    const mockGetCohort = jest.spyOn(cohortsApi(), 'getCohort');
    const { id, namespace } = workspaceDataStub;

    let { unmount } = component();
    await waitFor(() => expect(mockGetCohort).toHaveBeenCalledTimes(0));
    expect(screen.queryAllByTestId('includes-search-group').length).toBe(0);
    expect(screen.queryAllByTestId('excludes-search-group').length).toBe(0);
    unmount();

    // Call cohort with 2 includes groups
    unmount = component('cohortId=1').unmount;
    await waitFor(() =>
      expect(mockGetCohort).toHaveBeenCalledWith(namespace, id, 1)
    );
    expect(screen.getAllByTestId('includes-search-group').length).toBe(2);
    expect(screen.queryAllByTestId('excludes-search-group').length).toBe(0);
    unmount();

    // Call cohort with 2 includes groups and one excludes group
    component('cohortId=2');
    await waitFor(() =>
      expect(mockGetCohort).toHaveBeenCalledWith(namespace, id, 2)
    );
    expect(screen.getAllByTestId('includes-search-group').length).toBe(2);
    expect(screen.getAllByTestId('excludes-search-group').length).toBe(1);
  });
});
