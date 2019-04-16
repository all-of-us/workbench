import {mount} from 'enzyme';
import * as React from 'react';

import {ConceptHomepage} from 'app/views/concept-homepage/component';
import {waitOneTickAndUpdate} from 'testing/react-test-helpers';
import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {ConceptsApi, ConceptSetsApi, WorkspacesApi} from 'generated/fetch';
import {WorkspacesApiStub} from 'testing/stubs/workspaces-api-stub';
import {currentWorkspaceStore} from 'app/utils/navigation';
import {WorkspacesServiceStub} from 'testing/stubs/workspace-service-stub';
import {WorkspaceAccessLevel} from 'generated';
import {ConceptsApiStub} from 'testing/stubs/concepts-api-stub';
import {ConceptSetsApiStub} from 'testing/stubs/concept-sets-api-stub';

describe('ConceptHomepage', () => {

  beforeEach(() => {
    registerApiClient(WorkspacesApi, new WorkspacesApiStub());
    registerApiClient(ConceptsApi, new ConceptsApiStub());
    registerApiClient(ConceptSetsApi, new ConceptSetsApiStub());
    currentWorkspaceStore.next({
      ...WorkspacesServiceStub.stubWorkspace(),
      accessLevel: WorkspaceAccessLevel.OWNER,
    });
  });

  it('should render', () => {
    const wrapper = mount(<ConceptHomepage />);
    expect(wrapper).toBeTruthy();
  });

  it('should default to displaying concept cards with counts', async () => {
    const wrapper = mount(<ConceptHomepage />);
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="domain-box"]').length).toBeGreaterThan(0);
  });

  it('should toggle between concept search and concept set list page', async () => {
    const wrapper = mount(<ConceptHomepage />);
    await waitOneTickAndUpdate(wrapper);
    wrapper.find('[data-test-id="concept-sets-link"]').first().simulate('click');
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="card-name"]').length).toBeGreaterThan(0);
    wrapper.find('[data-test-id="concepts-link"]').first().simulate('click');
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="domain-box"]').length).toBeGreaterThan(0);
  });
});
