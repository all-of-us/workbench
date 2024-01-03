import { WorkspacesApi } from 'generated/fetch';

import { getTrail } from 'app/components/breadcrumb';
import {
  analysisTabName,
  analysisTabPath,
  appDisplayPath,
  dataTabPath,
} from 'app/routing/utils';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
import { currentWorkspaceStore } from 'app/utils/navigation';

import { cohortReviewStubs } from 'testing/stubs/cohort-review-service-stub';
import { exampleCohortStubs } from 'testing/stubs/cohorts-api-stub';
import { ConceptSetsApiStub } from 'testing/stubs/concept-sets-api-stub';
import { workspaceDataStub } from 'testing/stubs/workspaces';
import { WorkspacesApiStub } from 'testing/stubs/workspaces-api-stub';

import { UIAppType } from './apps-panel/utils';
import { BreadcrumbType } from './breadcrumb-type';

describe('getTrail', () => {
  beforeEach(() => {
    registerApiClient(WorkspacesApi, new WorkspacesApiStub());
    currentWorkspaceStore.next(workspaceDataStub);
  });

  it('works', () => {
    const ns = 'testns';
    const wsid = 'testwsid';
    const cid = '123';
    const crid = '456';
    const pid = '789';

    const trail = getTrail(
      BreadcrumbType.Participant,
      workspaceDataStub,
      exampleCohortStubs[0],
      cohortReviewStubs[0],
      ConceptSetsApiStub.stubConceptSets()[0],
      { ns, wsid, cid, crid, pid }
    );
    expect(trail.map((item) => item.label)).toEqual([
      'Workspaces',
      workspaceDataStub.name,
      cohortReviewStubs[0].cohortName,
      `Participant ${pid}`,
    ]);
    expect(trail[3].url).toEqual(
      dataTabPath('testns', 'testwsid') +
        `/cohorts/${cid}/reviews/${crid}/participants/${pid}`
    );
  });

  // regression test for RW-7572
  test.each(Object.keys(BreadcrumbType))(
    'handles breadcrumb type %s',
    (bType: string) => {
      const trail = getTrail(
        BreadcrumbType[bType],
        workspaceDataStub,
        exampleCohortStubs[0],
        cohortReviewStubs[0],
        ConceptSetsApiStub.stubConceptSets()[0],
        { ns: 'testns', wsid: 'testwsid', cid: '88', pid: '77' }
      );
      expect(trail.length).toBeGreaterThan(0);
    }
  );

  const analysisTabDisplay = `${analysisTabName[0].toUpperCase()}${analysisTabName
    .slice(1)
    .toLowerCase()}`;

  it('Should display correct trail for Jupyter', () => {
    const ns = 'testns';
    const wsid = 'testwsid';
    const nbName = 'myNotebook';

    const trail = getTrail(
      BreadcrumbType.Analysis,
      workspaceDataStub,
      undefined,
      undefined,
      undefined,
      { ns, wsid, nbName }
    );

    expect(trail.map((item) => item.label)).toEqual([
      'Workspaces',
      workspaceDataStub.name,
      analysisTabDisplay,
      nbName,
    ]);
    expect(trail[trail.length - 1].url).toEqual(
      `${analysisTabPath(ns, wsid)}/${nbName}`
    );
  });

  it('Should display correct trail for Jupyter preview', () => {
    const ns = 'testns';
    const wsid = 'testwsid';
    const nbName = 'myNotebook';

    const trail = getTrail(
      BreadcrumbType.AnalysisPreview,
      workspaceDataStub,
      undefined,
      undefined,
      undefined,
      { ns, wsid, nbName }
    );

    expect(trail.map((item) => item.label)).toEqual([
      'Workspaces',
      workspaceDataStub.name,
      analysisTabDisplay,
      nbName,
    ]);
    expect(trail[trail.length - 1].url).toEqual(
      `${analysisTabPath(ns, wsid)}/preview/${nbName}`
    );
  });

  it('Should display correct trail for User Apps', () => {
    const ns = 'testns';
    const wsid = 'testwsid';
    const nbName = "don't display this!";
    const appType = UIAppType.RSTUDIO;

    const trail = getTrail(
      BreadcrumbType.UserApp,
      workspaceDataStub,
      undefined,
      undefined,
      undefined,
      { ns, wsid, nbName, appType }
    );

    expect(trail.map((item) => item.label)).toEqual([
      'Workspaces',
      workspaceDataStub.name,
      analysisTabDisplay,
      UIAppType.RSTUDIO,
    ]);
    expect(trail[trail.length - 1].url).toEqual(
      appDisplayPath(ns, wsid, appType)
    );
  });
});
