import {mount} from 'enzyme';
import * as React from 'react';

import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {currentWorkspaceStore} from 'app/utils/navigation';
import {ConceptSetsApi, Domain, SurveyQuestions, Surveys} from 'generated/fetch';
import {waitOneTickAndUpdate} from 'testing/react-test-helpers';
import {ConceptSetsApiStub} from 'testing/stubs/concept-sets-api-stub';
import {ConceptStubVariables} from 'testing/stubs/concepts-api-stub';
import {workspaceDataStub} from 'testing/stubs/workspaces-api-stub';
import {ConceptSurveyAddModal} from './concept-survey-add-modal';

const surveyList: Array<SurveyQuestions> = [{
  question: 'Lifestyle',
  conceptId: 5
}];

describe('ConceptSurveyAddModal', () => {
  let props;
  let conceptSetsApi: ConceptSetsApiStub;
  const stubConcepts = ConceptStubVariables.STUB_CONCEPTS;

  const component = () => {
    return mount(<ConceptSurveyAddModal {...props}/>);
  };

  beforeEach(() => {
    props = {
      onSave: () => {},
      onClose: () => {},
      selectedSurvey: surveyList,
      surveyName: 'LIFESTYLE'
    };

    conceptSetsApi = new ConceptSetsApiStub();
    registerApiClient(ConceptSetsApi, conceptSetsApi);
    currentWorkspaceStore.next(workspaceDataStub);
  });


  it('displays option to add to existing concept set if survey concept set for survey exists',
    async() => {
      const wrapper = component();
      const stubSetsInDomain = conceptSetsApi.surveyConceptSets
        .filter(s => s.domain === Domain.OBSERVATION)
        .filter(s => s.survey === Surveys.LIFESTYLE)
        .map(s => s.name);
      await waitOneTickAndUpdate(wrapper);
      expect(wrapper.find('[data-test-id="add-to-existing"]').exists()).toBeTruthy();
      const foundSets = wrapper.find('[data-test-id="existing-set"]').map((s) => s.text());
      expect(foundSets).toEqual(stubSetsInDomain);
    });

  it('disables option to add to existing if concept set does not exist & defaults to create',
    async() => {
      props.surveyName = Surveys.THEBASICS.toString();
      const wrapper = component();
      await waitOneTickAndUpdate(wrapper);
      expect(wrapper.find('[data-test-id="add-to-existing"]').exists()).toBeFalsy();
      expect(wrapper.find('[data-test-id="create-new-set"]').exists()).toBeTruthy();
      expect(wrapper.find('[data-test-id="toggle-existing-set"]')
        .first().prop('disabled')).toBe(true);
    });

  it('allows user to toggle to create new set', async() => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="add-to-existing"]').exists()).toBeTruthy();
    wrapper.find('[data-test-id="toggle-new-set"]').first().simulate('click');
    expect(wrapper.find('[data-test-id="create-new-set"]').exists()).toBeTruthy();
  });

});
