import '@testing-library/jest-dom';

import { MemoryRouter } from 'react-router-dom';
import * as fp from 'lodash/fp';

import { WorkspacesApi } from 'generated/fetch';

import { render, screen } from '@testing-library/react';
import { UIAppType } from 'app/components/apps-panel/utils';
import {
  analysisTabName,
  analysisTabPath,
  appDisplayPath,
  dataTabPath,
} from 'app/routing/utils';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
import { minusDays, plusDays } from 'app/utils/dates';
import { currentWorkspaceStore } from 'app/utils/navigation';
import { profileStore, routeDataStore } from 'app/utils/stores';

import { cohortReviewStubs } from 'testing/stubs/cohort-review-service-stub';
import { exampleCohortStubs } from 'testing/stubs/cohorts-api-stub';
import { ConceptSetsApiStub } from 'testing/stubs/concept-sets-api-stub';
import { ProfileStubVariables } from 'testing/stubs/profile-api-stub';
import { workspaceDataStub } from 'testing/stubs/workspaces';
import { WorkspacesApiStub } from 'testing/stubs/workspaces-api-stub';

import { Breadcrumb, getTrail } from './breadcrumb';
import { BreadcrumbType } from './breadcrumb-type';

// Mock ReactDOM.createPortal since CreditBanner uses portals
jest.mock('react-dom', () => {
  return {
    ...jest.requireActual('react-dom'),
    createPortal: (element: any) => element,
  };
});

// Mock the CreditBanner component
jest.mock('app/lab/pages/workspace/initial-credits/credit-banner', () => {
  return {
    CreditBanner: () => (
      <div data-testid='credit-banner'>Credit Banner (Mocked)</div>
    ),
  };
});

describe('getTrail', () => {
  beforeEach(() => {
    registerApiClient(WorkspacesApi, new WorkspacesApiStub());
    currentWorkspaceStore.next(workspaceDataStub);
  });

  it('works', () => {
    const ns = 'testNs';
    const terraName = 'testTerraName';
    const cid = '123';
    const crid = '456';
    const pid = '789';

    const trail = getTrail(
      BreadcrumbType.Participant,
      workspaceDataStub,
      exampleCohortStubs[0],
      cohortReviewStubs[0],
      ConceptSetsApiStub.stubConceptSets()[0],
      { ns, terraName, cid, crid, pid }
    );
    expect(trail.map((item) => item.label)).toEqual([
      'Workspaces',
      workspaceDataStub.name,
      cohortReviewStubs[0].cohortName,
      `Participant ${pid}`,
    ]);
    expect(trail[3].url).toEqual(
      dataTabPath(ns, terraName) +
        `/cohorts/${cid}/reviews/${crid}/participants/${pid}`
    );
  });

  // regression test for RW-7572
  test.each(Object.keys(BreadcrumbType))(
    'handles breadcrumb type %s',
    (bType: string) => {
      const ns = 'testNs';
      const terraName = 'testTerraName';
      const cid = '88';
      const pid = '77';

      const trail = getTrail(
        BreadcrumbType[bType],
        workspaceDataStub,
        exampleCohortStubs[0],
        cohortReviewStubs[0],
        ConceptSetsApiStub.stubConceptSets()[0],
        { ns, terraName, cid, pid }
      );
      expect(trail.length).toBeGreaterThan(0);
    }
  );

  const analysisTabDisplay = `${analysisTabName[0].toUpperCase()}${analysisTabName
    .slice(1)
    .toLowerCase()}`;

  it('Should display correct trail for Jupyter', () => {
    const ns = 'testNs';
    const terraName = 'testTerraName';
    const nbName = 'myNotebook';

    const trail = getTrail(
      BreadcrumbType.Analysis,
      workspaceDataStub,
      undefined,
      undefined,
      undefined,
      { ns, terraName, nbName }
    );

    expect(trail.map((item) => item.label)).toEqual([
      'Workspaces',
      workspaceDataStub.name,
      analysisTabDisplay,
      nbName,
    ]);
    expect(trail[trail.length - 1].url).toEqual(
      `${analysisTabPath(ns, terraName)}/${nbName}`
    );
  });

  it('Should display correct trail for Jupyter preview', () => {
    const ns = 'testNs';
    const terraName = 'testTerraName';
    const nbName = 'myNotebook';

    const trail = getTrail(
      BreadcrumbType.AnalysisPreview,
      workspaceDataStub,
      undefined,
      undefined,
      undefined,
      { ns, terraName, nbName }
    );

    expect(trail.map((item) => item.label)).toEqual([
      'Workspaces',
      workspaceDataStub.name,
      analysisTabDisplay,
      nbName,
    ]);
    expect(trail[trail.length - 1].url).toEqual(
      `${analysisTabPath(ns, terraName)}/preview/${nbName}`
    );
  });

  it('Should display correct trail for User Apps', () => {
    const ns = 'testNs';
    const terraName = 'testTerraName';
    const nbName = "don't display this!";
    const appType = UIAppType.RSTUDIO;

    const trail = getTrail(
      BreadcrumbType.UserApp,
      workspaceDataStub,
      undefined,
      undefined,
      undefined,
      { ns, terraName, nbName, appType }
    );

    const trailLabels: string[] = trail.map((item) => item.label);
    expect(trailLabels).not.toContain(nbName);
    expect(trailLabels).toEqual([
      'Workspaces',
      workspaceDataStub.name,
      analysisTabDisplay,
      UIAppType.RSTUDIO,
    ]);
    expect(trail[trail.length - 1].url).toEqual(
      appDisplayPath(ns, terraName, appType)
    );
  });
});

describe('Breadcrumb Component', () => {
  const load = jest.fn();
  const reload = jest.fn();
  const updateCache = jest.fn();

  beforeEach(() => {
    registerApiClient(WorkspacesApi, new WorkspacesApiStub());
    routeDataStore.set({ breadcrumb: BreadcrumbType.Workspace });
  });

  it('should display CreditBanner when credits are exhausted', async () => {
    // Arrange
    const modifiedWorkspace = fp.cloneDeep(workspaceDataStub);
    modifiedWorkspace.creatorUser = {
      givenName: 'Test',
      familyName: 'User',
      userName: 'test@example.com',
    };
    modifiedWorkspace.initialCredits = {
      exhausted: true,
      expirationEpochMillis: plusDays(Date.now(), 1), // not expired
      expirationBypassed: false,
    };

    currentWorkspaceStore.next(modifiedWorkspace);

    profileStore.set({
      profile: ProfileStubVariables.PROFILE_STUB,
      load,
      reload,
      updateCache,
    });

    // Act
    render(
      <MemoryRouter>
        <Breadcrumb />
      </MemoryRouter>
    );

    // Assert
    expect(screen.queryByTestId('credit-banner')).not.toBeInTheDocument();
  });

  it('should display CreditBanner when credits are expired and not bypassed', async () => {
    // Arrange
    const modifiedWorkspace = fp.cloneDeep(workspaceDataStub);
    modifiedWorkspace.creatorUser = {
      givenName: 'Test',
      familyName: 'User',
      userName: 'test@example.com',
    };
    modifiedWorkspace.initialCredits = {
      exhausted: false,
      expirationEpochMillis: minusDays(Date.now(), 1), // expired
      expirationBypassed: false,
    };

    currentWorkspaceStore.next(modifiedWorkspace);

    profileStore.set({
      profile: ProfileStubVariables.PROFILE_STUB,
      load,
      reload,
      updateCache,
    });

    // Act
    render(
      <MemoryRouter>
        <Breadcrumb />
      </MemoryRouter>
    );

    // Assert
    expect(
      await screen.findByText('Credit Banner (Mocked)')
    ).toBeInTheDocument();
  });

  it('should not display CreditBanner when credits are expired but bypassed', () => {
    // Arrange
    const modifiedWorkspace = fp.cloneDeep(workspaceDataStub);
    modifiedWorkspace.creatorUser = {
      givenName: 'Test',
      familyName: 'User',
      userName: 'test@example.com',
    };
    modifiedWorkspace.initialCredits = {
      exhausted: false,
      expirationEpochMillis: minusDays(Date.now(), 1), // expired
      expirationBypassed: true, // but bypassed
    };

    currentWorkspaceStore.next(modifiedWorkspace);

    profileStore.set({
      profile: ProfileStubVariables.PROFILE_STUB,
      load,
      reload,
      updateCache,
    });

    // Act
    render(
      <MemoryRouter>
        <Breadcrumb />
      </MemoryRouter>
    );

    // Assert
    expect(screen.queryByTestId('credit-banner')).not.toBeInTheDocument();
  });

  it('should not display CreditBanner when credits are not expired and not exhausted', () => {
    // Arrange
    const modifiedWorkspace = fp.cloneDeep(workspaceDataStub);
    modifiedWorkspace.creatorUser = {
      givenName: 'Test',
      familyName: 'User',
      userName: 'test@example.com',
    };
    modifiedWorkspace.initialCredits = {
      exhausted: false,
      expirationEpochMillis: plusDays(Date.now(), 1), // not expired
      expirationBypassed: false,
    };

    currentWorkspaceStore.next(modifiedWorkspace);

    profileStore.set({
      profile: ProfileStubVariables.PROFILE_STUB,
      load,
      reload,
      updateCache,
    });

    // Act
    render(
      <MemoryRouter>
        <Breadcrumb />
      </MemoryRouter>
    );

    // Assert
    expect(screen.queryByTestId('credit-banner')).not.toBeInTheDocument();
  });
});
