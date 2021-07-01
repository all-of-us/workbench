import {mount} from 'enzyme';
import * as React from 'react';

import {ConceptHomepage} from 'app/pages/data/concept/concept-homepage';
import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {currentWorkspaceStore} from 'app/utils/navigation';
import {
  CohortBuilderApi,
  ConceptSetsApi,
  WorkspacesApi
} from 'generated/fetch';
import {waitOneTickAndUpdate} from 'testing/react-test-helpers';
import {CohortBuilderServiceStub, DomainStubVariables, SurveyStubVariables} from 'testing/stubs/cohort-builder-service-stub';
import {ConceptSetsApiStub} from 'testing/stubs/concept-sets-api-stub';
import {workspaceDataStub} from 'testing/stubs/workspaces';
import {WorkspacesApiStub} from 'testing/stubs/workspaces-api-stub';
import {Key} from 'ts-key-enum';

function searchTable(searchTerm: string, wrapper) {
  wrapper.find('[data-test-id="concept-search-input"]')
    .find('input').simulate('change', {target: {value: searchTerm}});
  wrapper.find('[data-test-id="concept-search-input"]')
    .find('input').simulate('keypress', {key: Key.Enter});
}

const defaultSearchTerm = 'test';

const component = () => {
  return mount(<ConceptHomepage setConceptSetUpdating={() => {}}
                                setShowUnsavedModal={() => {}}
                                setUnsavedConceptChanges={() => {}}
                                hideSpinner={() => {}}
                                showSpinner={() => {}}
                                spinnerVisible={false}/>);
}

describe('ConceptHomepage', () => {

  beforeEach(() => {
    registerApiClient(WorkspacesApi, new WorkspacesApiStub());
    registerApiClient(ConceptSetsApi, new ConceptSetsApiStub());
    registerApiClient(CohortBuilderApi, new CohortBuilderServiceStub());
    currentWorkspaceStore.next(workspaceDataStub);
  });

  it('should render', () => {
    const wrapper = component();
    expect(wrapper).toBeTruthy();
  });

  it('should have one card per domain.', async() => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="domain-box-name"]').length)
      .toBe(DomainStubVariables.STUB_DOMAINS.length);
  });

  it('should have one card per survey.', async() => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="survey-box-name"]').length)
      .toBe(SurveyStubVariables.STUB_SURVEYS.length);
  });

  // Need to update with new api calls
  /*it('should show warning and not search if invalid characters have been entered', async() => {
    const conceptSpy = jest.spyOn(conceptsApi(), 'searchConcepts');
    const countSpy = jest.spyOn(conceptsApi(), 'domainCounts');
    const surveySpy = jest.spyOn(conceptsApi(), 'searchSurveys');
    const wrapper = mount(<ConceptHomepage setConceptSetUpdating={() => {}}
                                           setShowUnsavedModal={() => {}}
                                           setUnsavedConceptChanges={() => {}}/>);
    await waitOneTickAndUpdate(wrapper);
    const termWithInvalidChars = defaultSearchTerm + '(';
    searchTable(termWithInvalidChars, wrapper);
    await waitOneTickAndUpdate(wrapper);

    // Test that it doesn't make any api calls
    expect(countSpy).toHaveBeenCalledTimes(0);
    expect(conceptSpy).toHaveBeenCalledTimes(0);
    expect(surveySpy).toHaveBeenCalledTimes(0);

    // Test that one input error alert is displayed with the proper message
    expect(wrapper.find('[data-test-id="input-error-alert"]').text()).toBe('There is an unclosed ( in the search string');
  });*/
});
