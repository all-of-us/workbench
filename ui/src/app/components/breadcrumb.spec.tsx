import {getTrail} from 'app/components/breadcrumb'
import {BreadcrumbType, currentWorkspaceStore, urlParamsStore} from 'app/utils/navigation';
import {registerApiClient} from "app/services/swagger-fetch-clients";

import {WorkspacesApi} from "generated/fetch";

import {exampleCohortStubs} from "testing/stubs/cohorts-api-stub";
import {ConceptSetsApiStub} from "testing/stubs/concept-sets-api-stub";
import {workspaceDataStub, WorkspacesApiStub, WorkspaceStubVariables} from 'testing/stubs/workspaces-api-stub';

describe('getTrail', () => {
  beforeEach(() => {
    registerApiClient(WorkspacesApi, new WorkspacesApiStub());
    urlParamsStore.next({
      ns: WorkspaceStubVariables.DEFAULT_WORKSPACE_NS,
      wsid: WorkspaceStubVariables.DEFAULT_WORKSPACE_ID
    });
    currentWorkspaceStore.next(workspaceDataStub);
  });

  it('works', () => {
    const trail = getTrail(BreadcrumbType.Participant,
      workspaceDataStub,
      exampleCohortStubs[0],
      ConceptSetsApiStub.stubConceptSets()[0],
      {ns: 'testns', wsid: 'testwsid', cid: 88, pid: 77}
    );
    expect(trail.map(item => item.label))
      .toEqual(['Workspaces', 'defaultWorkspace', 'sample name', 'Participant 77']);
    expect(trail[3].url)
      .toEqual('/workspaces/testns/testwsid/data/cohorts/88/review/participants/77');
  });
});
