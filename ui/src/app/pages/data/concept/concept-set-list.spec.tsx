import {mount} from 'enzyme';
import * as React from 'react';

import {ConceptSetsList} from 'app/pages/data/concept/concept-set-list';

import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {currentWorkspaceStore} from 'app/utils/navigation';
import {ConceptsApi, ConceptSetsApi, WorkspacesApi} from 'generated/fetch';
import {ConceptsApiStub} from 'testing/stubs/concepts-api-stub';
import {ConceptSetsApiStub} from 'testing/stubs/concept-sets-api-stub';
import {WorkspacesApiStub, workspaceDataStub} from 'testing/stubs/workspaces-api-stub';

import {waitOneTickAndUpdate} from 'testing/react-test-helpers';

describe('ConceptSetList', () => {
  beforeEach(() => {
    registerApiClient(ConceptSetsApi, new ConceptSetsApiStub());
    registerApiClient(WorkspacesApi, new WorkspacesApiStub());
    registerApiClient(ConceptsApi, new ConceptsApiStub());
    currentWorkspaceStore.next(workspaceDataStub);
  });
  it('should render', () => {
    const wrapper = mount(<ConceptSetsList />);
    expect(wrapper).toBeTruthy();
  });
  it('displays correct concept sets', async () => {
    const wrapper = mount(<ConceptSetsList />);
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="card-name"]').map(c => c.text()))
      .toEqual(ConceptSetsApiStub.stubConceptSets().map(c => c.name));
  });

  // Note: this spec is not testing the Popup menus on resource cards due to an issue using
  //    PopupTrigger in the test suite.
});
