import {mount} from 'enzyme';
import * as React from 'react';

import {ConceptWrapper} from 'app/views/concepts/component';
import {waitOneTickAndUpdate} from 'testing/react-test-helpers';
import {conceptsApi, registerApiClient} from 'app/services/swagger-fetch-clients';
import {ConceptsApi, ConceptSetsApi, StandardConceptFilter, WorkspacesApi} from 'generated/fetch';
import {WorkspacesApiStub, WorkspaceStubVariables} from 'testing/stubs/workspaces-api-stub';
import {currentWorkspaceStore} from 'app/utils/navigation';
import {WorkspacesServiceStub} from 'testing/stubs/workspace-service-stub';
import {WorkspaceAccessLevel} from 'generated';
import {DomainInfo} from 'generated/fetch';
import {ConceptsApiStub, ConceptStubVariables, DomainStubVariables} from 'testing/stubs/concepts-api-stub';
import {ConceptSetsApiStub} from 'testing/stubs/concept-sets-api-stub';


function isSelectedDomain(
  domain: DomainInfo, wrapper): boolean {
  return wrapper.find('[data-test-id="active-domain"]').key() === domain.domain;
}

function conceptsCountInDomain(domain: DomainInfo, isStandardConcepts: boolean): number {
  const conceptsInDomain = ConceptStubVariables.STUB_CONCEPTS
    .filter(c => c.domainId === domain.name);
  if (isStandardConcepts) {
    return conceptsInDomain.filter(c => c.standardConcept === isStandardConcepts).length;
  } else {
    return conceptsInDomain.length;
  }
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
    const spy = jest.spyOn(conceptsApi(), 'searchConcepts');
    const wrapper = mount(<ConceptWrapper />);
    await waitOneTickAndUpdate(wrapper);
    const searchInput = wrapper.find('[data-test-id="concept-search-input"]')
      .find('input').getDOMNode() as HTMLInputElement;
    searchInput.value = searchTerm;
    wrapper.find('[data-test-id="concept-search-input"]')
      .find('input').simulate('keydown', {keyCode: 13});
    await waitOneTickAndUpdate(wrapper);

    DomainStubVariables.STUB_DOMAINS.forEach((domain) => {
      const includeDomainCounts = isSelectedDomain(domain, wrapper);
      const expectedRequest = {
        query: searchTerm,
        // Tests that it searches only standard concepts.
        standardConceptFilter: StandardConceptFilter.STANDARDCONCEPTS,
        domain: domain.domain,
        includeDomainCounts: includeDomainCounts,
        includeVocabularyCounts: true,
        maxResults: 100
      };
      expect(spy).toHaveBeenCalledWith(
        WorkspaceStubVariables.DEFAULT_WORKSPACE_NS,
        WorkspaceStubVariables.DEFAULT_WORKSPACE_ID,
        expectedRequest
      );
    });

    // Test that it makes a call for each domain
    expect(spy).toHaveBeenCalledTimes(DomainStubVariables.STUB_DOMAINS.length);

    // Test that it switches to the table view
    expect(wrapper.find('[data-test-id="conceptTable"]').length).toBeGreaterThan(0);
    const firstDomainRowName = wrapper.find('[data-test-id="conceptName"]').at(1).text();
    await waitOneTickAndUpdate(wrapper);

    // Test that it changes the table when a new domain is selected
    const unselectedDomainName = DomainStubVariables.STUB_DOMAINS[1].name;
    wrapper.find('[data-test-id="domain-header-' + unselectedDomainName + '"]')
      .first().simulate('click');
    expect( wrapper.find('[data-test-id="conceptName"]').at(1).text())
      .not.toBe(firstDomainRowName);

  });

  it('should changes search criteria when standard only not checked', async () => {
    const spy = jest.spyOn(conceptsApi(), 'searchConcepts');
    const searchTerm = 'test';
    const selectedDomain = DomainStubVariables.STUB_DOMAINS[1];
    const wrapper = mount(<ConceptWrapper />);
    await waitOneTickAndUpdate(wrapper);

    wrapper.find('[data-test-id="standardConceptsCheckBox"]').first()
      .simulate('change', { target: { checked: true } });
    await waitOneTickAndUpdate(wrapper);
    const searchInput = wrapper.find('[data-test-id="concept-search-input"]')
      .find('input').getDOMNode() as HTMLInputElement;
    searchInput.value = searchTerm;
    wrapper.find('[data-test-id="concept-search-input"]')
      .find('input').simulate('keydown', {keyCode: 13});
    await waitOneTickAndUpdate(wrapper);

    DomainStubVariables.STUB_DOMAINS.forEach((domain) => {
      const includeDomainCounts = isSelectedDomain(domain, wrapper);
      const expectedRequest = {
        query: searchTerm,
        // Tests that it searches only standard concepts.
        standardConceptFilter: StandardConceptFilter.ALLCONCEPTS,
        domain: domain.domain,
        includeDomainCounts: includeDomainCounts,
        includeVocabularyCounts: true,
        maxResults: 100
      };
      expect(spy).toHaveBeenCalledWith(
        WorkspaceStubVariables.DEFAULT_WORKSPACE_NS,
        WorkspaceStubVariables.DEFAULT_WORKSPACE_ID,
        expectedRequest
      );
    });
    // check number of rows in table minus header rows
    expect( wrapper.find('[data-test-id="conceptName"]').length)
      .toBe(conceptsCountInDomain(selectedDomain, false) - 1);
  });

  // it('should display the selected concepts on header', () => {
  //   // TODO: test toggling tabs
  // });
  //
  // it('should display the selected concepts on sliding button', () => {
  //   // TODO
  // });
  //
  // it('should clear search and selected concepts', () => {
  //   // TODO
  // });
  //
  // it('should clear selected concepts after adding', () => {
  //   // TODO
  // });

});
