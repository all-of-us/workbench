import {mount} from 'enzyme';
import * as React from 'react';

import {ConceptHomepage} from 'app/pages/data/concept/concept-homepage';
import {conceptsApi, registerApiClient} from 'app/services/swagger-fetch-clients';
import {currentWorkspaceStore} from 'app/utils/navigation';
import {
  ConceptsApi,
  ConceptSetsApi,
  DomainInfo,
  StandardConceptFilter,
  WorkspacesApi
} from 'generated/fetch';
import {waitOneTickAndUpdate} from 'testing/react-test-helpers';
import {ConceptSetsApiStub} from 'testing/stubs/concept-sets-api-stub';
import {
  ConceptsApiStub,
  ConceptStubVariables,
  DomainStubVariables,
  SurveyStubVariables
} from 'testing/stubs/concepts-api-stub';
import {
  workspaceDataStub,
  WorkspacesApiStub,
  WorkspaceStubVariables
} from 'testing/stubs/workspaces-api-stub';
import {Key} from 'ts-key-enum';


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

function searchTable(searchTerm: string, wrapper) {
  wrapper.find('[data-test-id="concept-search-input"]')
    .find('input').simulate('change', {target: {value: searchTerm}});
  wrapper.find('[data-test-id="concept-search-input"]')
    .find('input').simulate('keypress', {key: Key.Enter});
}

const defaultSearchTerm = 'test';

describe('ConceptHomepage', () => {

  beforeEach(() => {
    registerApiClient(WorkspacesApi, new WorkspacesApiStub());
    registerApiClient(ConceptsApi, new ConceptsApiStub());
    registerApiClient(ConceptSetsApi, new ConceptSetsApiStub());
    currentWorkspaceStore.next(workspaceDataStub);
  });

  it('should render', () => {
    const wrapper = mount(<ConceptHomepage />);
    expect(wrapper).toBeTruthy();
  });

  it('should have one card per domain.', async() => {
    const wrapper = mount(<ConceptHomepage />);
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="domain-box-name"]').length)
      .toBe(DomainStubVariables.STUB_DOMAINS.length);
  });

  it('should have one card per survey.', async() => {
    const wrapper = mount(<ConceptHomepage />);
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="survey-box-name"]').length)
      .toBe(SurveyStubVariables.STUB_SURVEYS.length);
  });

  it('should default to standard concepts only, and performs a count call and full search', async() => {
    const conceptSpy = jest.spyOn(conceptsApi(), 'searchConcepts');
    const countSpy = jest.spyOn(conceptsApi(), 'domainCounts');
    const surveySpy = jest.spyOn(conceptsApi(), 'searchSurveys');
    const wrapper = mount(<ConceptHomepage />);
    await waitOneTickAndUpdate(wrapper);
    searchTable(defaultSearchTerm, wrapper);
    await waitOneTickAndUpdate(wrapper);
    const request = {
      query: defaultSearchTerm,
      // Tests that it searches only standard concepts.
      standardConceptFilter: StandardConceptFilter.STANDARDCONCEPTS,
      maxResults: 1000
    };

    // Tests that the domain tab counts are called
    expect(countSpy).toHaveBeenCalledWith(
      WorkspaceStubVariables.DEFAULT_WORKSPACE_NS,
      WorkspaceStubVariables.DEFAULT_WORKSPACE_ID,
      request
    );
    expect(countSpy).toHaveBeenCalledTimes(1);

    DomainStubVariables.STUB_DOMAINS.forEach((domain) => {
      const expectedRequest = {...request, domain: domain.domain};
      expect(conceptSpy).toHaveBeenCalledWith(
        WorkspaceStubVariables.DEFAULT_WORKSPACE_NS,
        WorkspaceStubVariables.DEFAULT_WORKSPACE_ID,
        expectedRequest
      );
    });

    // Test that it makes a call for each domain.
    expect(conceptSpy).toHaveBeenCalledTimes(DomainStubVariables.STUB_DOMAINS.length);

    // Test that it makes a separate call for surveys.
    expect(surveySpy).toHaveBeenCalledWith(
      WorkspaceStubVariables.DEFAULT_WORKSPACE_NS,
      WorkspaceStubVariables.DEFAULT_WORKSPACE_ID,
      request
    );
    expect(surveySpy).toHaveBeenCalledTimes(1);

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

  it('should change search criteria when standard only not checked', async() => {
    const spy = jest.spyOn(conceptsApi(), 'searchConcepts');
    const selectedDomain = DomainStubVariables.STUB_DOMAINS[1];
    const wrapper = mount(<ConceptHomepage />);
    await waitOneTickAndUpdate(wrapper);

    wrapper.find('[data-test-id="standardConceptsCheckBox"] input').first()
      .simulate('change', { target: { checked: false } });
    await waitOneTickAndUpdate(wrapper);
    searchTable(defaultSearchTerm, wrapper);
    await waitOneTickAndUpdate(wrapper);

    DomainStubVariables.STUB_DOMAINS.forEach((domain) => {
      const expectedRequest = {
        query: defaultSearchTerm,
        // Tests that it searches only standard concepts.
        standardConceptFilter: StandardConceptFilter.ALLCONCEPTS,
        domain: domain.domain,
        maxResults: 1000
      };
      expect(spy).toHaveBeenCalledWith(
        WorkspaceStubVariables.DEFAULT_WORKSPACE_NS,
        WorkspaceStubVariables.DEFAULT_WORKSPACE_ID,
        expectedRequest
      );
    });
    // check number of rows in table plus header row
    expect(wrapper.find('[data-test-id="conceptName"]').length)
      .toBe(conceptsCountInDomain(selectedDomain, false) + 1);
  });

  it('should display the selected concepts on header', async() => {
    const wrapper = mount(<ConceptHomepage />);
    await waitOneTickAndUpdate(wrapper);
    searchTable(defaultSearchTerm, wrapper);
    await waitOneTickAndUpdate(wrapper);

    wrapper.find('span.p-checkbox-icon.p-clickable').at(1).simulate('click');
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="selectedConcepts"]').text()).toBe('1');
  });

  it('should display the selected concepts on sliding button', async() => {
    const wrapper = mount(<ConceptHomepage />);
    await waitOneTickAndUpdate(wrapper);
    searchTable(defaultSearchTerm, wrapper);
    await waitOneTickAndUpdate(wrapper);

    // before anything is selected, the sliding button should be disabled
    expect(wrapper.find('[data-test-id="sliding-button"]')
      .parent().props()['disable']).toBeTruthy();

    wrapper.find('span.p-checkbox-icon.p-clickable').at(1).simulate('click');
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="sliding-button"]')
      .parent().props()['disable']).toBeFalsy();
    expect(wrapper.find('[data-test-id="sliding-button"]').text()).toBe('Add (1) to set');
  });

  it('should clear search and selected concepts', async() => {
    const wrapper = mount(<ConceptHomepage />);
    await waitOneTickAndUpdate(wrapper);
    searchTable(defaultSearchTerm, wrapper);
    await waitOneTickAndUpdate(wrapper);

    wrapper.find('span.p-checkbox-icon.p-clickable').at(1).simulate('click');
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="selectedConcepts"]').text()).toBe('1');

    wrapper.find('[data-test-id="clear-search"]').first().simulate('click');
    expect(wrapper.find('[data-test-id="selectedConcepts"]').length).toEqual(0);
  });

  it('should clear search box and reset page to default view', async() => {
    const wrapper = mount(<ConceptHomepage />);
    await waitOneTickAndUpdate(wrapper);
    searchTable(defaultSearchTerm, wrapper);
    await waitOneTickAndUpdate(wrapper);

    expect(wrapper.find('[data-test-id="concept-search-input"]')
      .find('input').props().value).toBe(defaultSearchTerm);
    wrapper.find('[data-test-id="clear-search"]').first().simulate('click');

    expect(wrapper.find('[data-test-id="concept-search-input"]')
      .find('input').props().value).toBe('');

    // Verify that the page resets to show the default view
    expect(wrapper.find('[data-test-id="domain-box-name"]').length)
      .toBe(DomainStubVariables.STUB_DOMAINS.length);
    expect(wrapper.find('[data-test-id="survey-box-name"]').length)
      .toBe(SurveyStubVariables.STUB_SURVEYS.length);

  });

  it('should display all Concept selected message when header checkbox is selected', async() => {
    const wrapper = mount(<ConceptHomepage />);
    await waitOneTickAndUpdate(wrapper);
    searchTable('headerText', wrapper);
    await waitOneTickAndUpdate(wrapper);

    // Select header checkbox
    wrapper.find('span.p-checkbox-icon.p-clickable').first().simulate('click');
    await waitOneTickAndUpdate(wrapper);

    expect(wrapper.find('[data-test-id="selection"]').text())
      .toContain('You’ve selected all 20 concepts.');
    expect(wrapper.find('[data-test-id="banner-link"]').text())
      .toBe('Select all 41 concepts');

    // Select the "Select All Concept" link
    wrapper.find('[data-test-id="banner-link"]').simulate('click');
    expect(wrapper.find('[data-test-id="selectedConcepts"]').length).toBe(1);

    // Should have link to clear all selection
    expect(wrapper.find('[data-test-id="selection"]').text())
      .toContain('You’ve selected all 41 concepts.');
    expect(wrapper.find('[data-test-id="banner-link"]').text())
      .toEqual('Clear Selection');

    // Select "Clear all" should end up with 0 selectedConcepts
    wrapper.find('[data-test-id="banner-link"]').simulate('click');
    expect(wrapper.find('[data-test-id="selectedConcepts"]').length).toBe(0);

  });
});
