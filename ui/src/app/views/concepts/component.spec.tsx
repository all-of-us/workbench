import {mount} from 'enzyme';
import * as React from 'react';

import {ConceptWrapper} from 'app/views/concepts/component';
import {waitOneTickAndUpdate} from 'testing/react-test-helpers';
import {conceptsApi, registerApiClient} from 'app/services/swagger-fetch-clients';
import {ConceptsApi, ConceptSetsApi, WorkspacesApi} from 'generated/fetch';
import {WorkspacesApiStub} from 'testing/stubs/workspaces-api-stub';
import {currentWorkspaceStore} from 'app/utils/navigation';
import {WorkspacesServiceStub, WorkspaceStubVariables} from 'testing/stubs/workspace-service-stub';
import {StandardConceptFilter, WorkspaceAccessLevel} from 'generated';
import {DomainInfo} from 'generated/fetch';
import {ConceptsApiStub, DomainStubVariables} from 'testing/stubs/concepts-api-stub';
import {ConceptSetsApiStub} from 'testing/stubs/concept-sets-api-stub';


function isSelectedDomain(
  domain: DomainInfo, wrapper): boolean {
  console.log(wrapper.find('[data-test-id="active-domain"]').parent().parent().debug());
    // .find('[data-test-id="domain-name"]').text());
  return true;
}

describe('ConceptWrapper', () => {

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
    const wrapper = mount(<ConceptWrapper />);
    expect(wrapper).toBeTruthy();
  });

  it('should have one card per domain.', async () => {
    const wrapper = mount(<ConceptWrapper />);
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="domain-box-name"]').length)
      .toBe(DomainStubVariables.STUB_DOMAINS.length);
  });

  it('should default to standard concepts only, and performs a full search', async () => {
    const searchTerm = 'test';
    const spy = jest.spyOn(conceptsApi(), 'getDomainInfo');
    const wrapper = mount(<ConceptWrapper />);
    const searchInput = wrapper.find('[data-test-id="concept-search-input"]')
      .find('input').getDOMNode() as HTMLInputElement;
    searchInput.value = searchTerm;
    wrapper.find('[data-test-id="concept-search-input"]')
      .find('input').simulate('keydown', {keyCode: 13});
    await waitOneTickAndUpdate(wrapper);
    // TODO
  });

  it('should changes search criteria when standard only not checked', () => {
    // TODO
  });

  it('should display the selected concepts on header', () => {
    // TODO: test toggling tabs
  });

  it('should display the selected concepts on sliding button', () => {
    // TODO
  });

  it('should clear search and selected concepts', () => {
    // TODO
  });

  it('should clear selected concepts after adding', () => {
    // TODO
  });

});
