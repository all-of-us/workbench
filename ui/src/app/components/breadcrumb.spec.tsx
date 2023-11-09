import { WorkspacesApi } from 'generated/fetch';

import { getTrail } from 'app/components/breadcrumb';
import { dataTabPath } from 'app/routing/utils';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
import { currentWorkspaceStore } from 'app/utils/navigation';

import { cohortReviewStubs } from 'testing/stubs/cohort-review-service-stub';
import { exampleCohortStubs } from 'testing/stubs/cohorts-api-stub';
import { ConceptSetsApiStub } from 'testing/stubs/concept-sets-api-stub';
import { workspaceDataStub } from 'testing/stubs/workspaces';
import { WorkspacesApiStub } from 'testing/stubs/workspaces-api-stub';

import { BreadcrumbType } from './breadcrumb-type';

describe('getTrail', () => {
  beforeEach(() => {
    registerApiClient(WorkspacesApi, new WorkspacesApiStub());
    currentWorkspaceStore.next(workspaceDataStub);
  });

  it('works', () => {
    const trail = getTrail(
      BreadcrumbType.Participant,
      workspaceDataStub,
      exampleCohortStubs[0],
      cohortReviewStubs[0],
      ConceptSetsApiStub.stubConceptSets()[0],
      { ns: 'testns', wsid: 'testwsid', cid: '88', crid: '99', pid: '77' }
    );
    expect(trail.map((item) => item.label)).toEqual([
      'Workspaces',
      'defaultWorkspace',
      'Cohort Name',
      'Participant 77',
    ]);
    expect(trail[3].url).toEqual(
      dataTabPath('testns', 'testwsid') +
        '/cohorts/88/reviews/99/participants/77'
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
});
