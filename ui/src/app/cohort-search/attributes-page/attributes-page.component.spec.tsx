import {mount, ReactWrapper, ShallowWrapper} from 'enzyme';
import {Dropdown} from 'primereact/dropdown';
import * as React from 'react';

import {ppiQuestions, ppiSurveys} from 'app/cohort-search/search-state.service';
import {cohortBuilderApi, registerApiClient} from 'app/services/swagger-fetch-clients';
import {currentWorkspaceStore} from 'app/utils/navigation';
import {CohortBuilderApi, Operator} from 'generated/fetch';
import SpyInstance = jest.SpyInstance;
import {waitOneTickAndUpdate} from 'testing/react-test-helpers';
import {
  CohortBuilderServiceStub,
  CriteriaWithAttributesStubVariables,
  RootSurveyStubVariables,
  SurveyQuestionStubVariables
} from 'testing/stubs/cohort-builder-service-stub'
import {workspaceDataStub} from 'testing/stubs/workspaces-api-stub';
import {AttributesPage, Props} from './attributes-page.component';

type AnyWrapper = (ShallowWrapper|ReactWrapper);

let props: Props;
let mockCountParticipants: SpyInstance;
let mockFindCriteriaAttributeByConceptId: SpyInstance;
let mockFindSurveyVersionByQuestionConceptId: SpyInstance;
let mockFindSurveyVersionByQuestionConceptIdAndAnswerConceptId: SpyInstance;

function component(): ReactWrapper {
  return mount(<AttributesPage {...props}/>);
}

function getNumericalDropdown(wrapper: AnyWrapper, index: string): Dropdown {
  return wrapper.find(`Dropdown[data-test-id="numerical-dropdown-${index}"]`).instance() as Dropdown;
}

function getNumericalInput(wrapper: AnyWrapper, index: string): AnyWrapper {
  return wrapper.find(`[data-test-id="numerical-input-${index}-0"]`).hostNodes();
}

describe('AttributesPageV2', () => {
  beforeEach(() => {
    currentWorkspaceStore.next(workspaceDataStub);
    registerApiClient(CohortBuilderApi, new CohortBuilderServiceStub());
    mockCountParticipants = jest.spyOn(cohortBuilderApi(), 'countParticipants');
    mockFindCriteriaAttributeByConceptId = jest.spyOn(cohortBuilderApi(), 'findCriteriaAttributeByConceptId');
    mockFindSurveyVersionByQuestionConceptId = jest.spyOn(cohortBuilderApi(), 'findSurveyVersionByQuestionConceptId');
    mockFindSurveyVersionByQuestionConceptIdAndAnswerConceptId = jest.spyOn(cohortBuilderApi(), 'findSurveyVersionByQuestionConceptIdAndAnswerConceptId');
    props = {
      back: () => {},
      close: () => {},
      criteria: [],
      node: CriteriaWithAttributesStubVariables[0],
      workspace: workspaceDataStub
    };
  });

  it('should render', () => {
    const wrapper = component();
    expect(wrapper.exists()).toBeTruthy();
  });

  it('should not call api and render a single dropdown for Height in Physical Measurements', async() => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(mockCountParticipants).toHaveBeenCalledTimes(0);
    expect(mockFindCriteriaAttributeByConceptId).toHaveBeenCalledTimes(0);
    expect(mockFindSurveyVersionByQuestionConceptId).toHaveBeenCalledTimes(0);
    expect(mockFindSurveyVersionByQuestionConceptIdAndAnswerConceptId).toHaveBeenCalledTimes(0);
    expect(wrapper.find('[data-test-id="numerical-dropdown-0"]').length).toBe(1);
    expect(wrapper.find('[data-test-id="numerical-dropdown-1"]').length).toBe(0);
    expect(wrapper.find('[data-test-id="numerical-input-0-0"]').length).toBe(0);
    expect(wrapper.find('[data-test-id="attributes-add-btn"]').first().props().disabled).toBeFalsy();
  });

  it('should not call api and render two dropdowns for BP in Physical Measurements', async() => {
    props.node = CriteriaWithAttributesStubVariables[1];
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(mockCountParticipants).toHaveBeenCalledTimes(0);
    expect(mockFindCriteriaAttributeByConceptId).toHaveBeenCalledTimes(0);
    expect(mockFindSurveyVersionByQuestionConceptId).toHaveBeenCalledTimes(0);
    expect(mockFindSurveyVersionByQuestionConceptIdAndAnswerConceptId).toHaveBeenCalledTimes(0);
    expect(wrapper.find('[data-test-id="numerical-dropdown-0"]').length).toBe(1);
    expect(wrapper.find('[data-test-id="numerical-dropdown-1"]').length).toBe(1);
    expect(wrapper.find('[data-test-id="numerical-input-0-0"]').length).toBe(0);
    expect(wrapper.find('[data-test-id="attributes-add-btn"]').first().props().disabled).toBeFalsy();
  });

  it('should call api for attributes for Labs and Measurements nodes', async() => {
    props.node = CriteriaWithAttributesStubVariables[2];
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(mockCountParticipants).toHaveBeenCalledTimes(0);
    expect(mockFindCriteriaAttributeByConceptId).toHaveBeenCalledTimes(1);
    expect(mockFindSurveyVersionByQuestionConceptId).toHaveBeenCalledTimes(0);
    expect(mockFindSurveyVersionByQuestionConceptIdAndAnswerConceptId).toHaveBeenCalledTimes(0);
  });

  it('should call api for attributes for non COPE Survey nodes', async() => {
    ppiSurveys.next({[workspaceDataStub.cdrVersionId]: RootSurveyStubVariables});
    ppiQuestions.next(SurveyQuestionStubVariables);
    props.node = CriteriaWithAttributesStubVariables[3];
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(mockCountParticipants).toHaveBeenCalledTimes(0);
    expect(mockFindCriteriaAttributeByConceptId).toHaveBeenCalledTimes(1);
    expect(mockFindSurveyVersionByQuestionConceptId).toHaveBeenCalledTimes(0);
    expect(mockFindSurveyVersionByQuestionConceptIdAndAnswerConceptId).toHaveBeenCalledTimes(0);
  });

  it('should call api for survey versions for COPE questions', async() => {
    ppiSurveys.next({[workspaceDataStub.cdrVersionId]: RootSurveyStubVariables});
    ppiQuestions.next(SurveyQuestionStubVariables);
    props.node = CriteriaWithAttributesStubVariables[4];
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(mockCountParticipants).toHaveBeenCalledTimes(0);
    expect(mockFindCriteriaAttributeByConceptId).toHaveBeenCalledTimes(0);
    expect(mockFindSurveyVersionByQuestionConceptId).toHaveBeenCalledTimes(1);
    expect(mockFindSurveyVersionByQuestionConceptIdAndAnswerConceptId).toHaveBeenCalledTimes(0);
  });

  it('should call api for attributes and survey versions (Question and Answer conceptId call) for COPE answers', async() => {
    ppiSurveys.next({[workspaceDataStub.cdrVersionId]: RootSurveyStubVariables});
    ppiQuestions.next(SurveyQuestionStubVariables);
    props.node = CriteriaWithAttributesStubVariables[5];
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(mockCountParticipants).toHaveBeenCalledTimes(0);
    expect(mockFindCriteriaAttributeByConceptId).toHaveBeenCalledTimes(1);
    expect(mockFindSurveyVersionByQuestionConceptId).toHaveBeenCalledTimes(0);
    expect(mockFindSurveyVersionByQuestionConceptIdAndAnswerConceptId).toHaveBeenCalledTimes(1);
  });

  it('should call api for attributes and survey versions (Question conceptId call) for COPE Select a Value answers', async() => {
    ppiSurveys.next({[workspaceDataStub.cdrVersionId]: RootSurveyStubVariables});
    ppiQuestions.next(SurveyQuestionStubVariables);
    props.node = CriteriaWithAttributesStubVariables[6];
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(mockCountParticipants).toHaveBeenCalledTimes(0);
    expect(mockFindCriteriaAttributeByConceptId).toHaveBeenCalledTimes(1);
    expect(mockFindSurveyVersionByQuestionConceptId).toHaveBeenCalledTimes(1);
    expect(mockFindSurveyVersionByQuestionConceptIdAndAnswerConceptId).toHaveBeenCalledTimes(0);
  });

  it('should render a single input for EQUAL operator and disable calculate button when empty', async() => {
    const wrapper = component();
    const numericalDropdown = getNumericalDropdown(wrapper, '0');
    numericalDropdown.props.onChange({
      originalEvent: undefined, value: Operator.EQUAL, target: {id: '', name: '', value: Operator.EQUAL}
    });
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="numerical-input-0-0"]').length).toBe(2);
    expect(wrapper.find('[data-test-id="attributes-calculate-btn"]').first().props().disabled).toBeTruthy();
    const numericalInput = getNumericalInput(wrapper, '0');
    numericalInput.simulate('change', {target: {value: 100}});
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="attributes-calculate-btn"]').first().props().disabled).toBeFalsy();
    wrapper.find('[data-test-id="attributes-calculate-btn"]').simulate('click');
    expect(mockCountParticipants).toHaveBeenCalledTimes(1);
  });
});
