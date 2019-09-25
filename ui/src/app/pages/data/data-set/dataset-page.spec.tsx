import {mount} from 'enzyme';
import * as React from 'react';

import {Button, Clickable} from 'app/components/buttons';
import {DataSetPage} from 'app/pages/data/data-set/dataset-page';
import {dataSetApi, registerApiClient} from 'app/services/swagger-fetch-clients';
import {currentWorkspaceStore, NavStore, urlParamsStore} from 'app/utils/navigation';
import {
  CohortsApi,
  ConceptsApi,
  ConceptSetsApi,
  DataSetApi,
  WorkspaceAccessLevel
} from 'generated/fetch';
import {waitOneTickAndUpdate} from 'testing/react-test-helpers';
import {CohortsApiStub, exampleCohortStubs} from 'testing/stubs/cohorts-api-stub';
import {ConceptSetsApiStub} from 'testing/stubs/concept-sets-api-stub';
import {ConceptsApiStub} from 'testing/stubs/concepts-api-stub';
import {DataSetApiStub} from 'testing/stubs/data-set-api-stub';
import {
  workspaceDataStub,
  workspaceStubs,
  WorkspaceStubVariables
} from 'testing/stubs/workspaces-api-stub';

describe('DataSet', () => {
  beforeEach(() => {
    registerApiClient(CohortsApi, new CohortsApiStub());
    registerApiClient(ConceptsApi, new ConceptsApiStub());
    registerApiClient(ConceptSetsApi, new ConceptSetsApiStub());
    registerApiClient(DataSetApi, new DataSetApiStub());
    urlParamsStore.next({
      ns: WorkspaceStubVariables.DEFAULT_WORKSPACE_NS,
      wsid: WorkspaceStubVariables.DEFAULT_WORKSPACE_ID
    });
    currentWorkspaceStore.next(workspaceDataStub);
  });

  it('should render', async() => {
    const wrapper = mount(<DataSetPage />);
    await waitOneTickAndUpdate(wrapper);
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.exists()).toBeTruthy();
  });

  it ('should display all concepts sets in workspace', async() => {
    const wrapper = mount(<DataSetPage />);
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="concept-set-list-item"]').length)
      .toBe(ConceptSetsApiStub.stubConceptSets().length);
  });

  it('should display all cohorts in workspace', async() => {
    const wrapper = mount(<DataSetPage />);
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="cohort-list-item"]').length)
      .toBe(exampleCohortStubs.length);
  });

  it('should display values based on Domain of Concept selected in workspace', async() => {
    const wrapper = mount(<DataSetPage />);
    await waitOneTickAndUpdate(wrapper);
    await waitOneTickAndUpdate(wrapper);

    // First Concept set in concept set list has domain "Condition"
    const condition_concept = wrapper.find('[data-test-id="concept-set-list-item"]').first()
        .find('input').first();
    condition_concept.simulate('change');
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="value-list-items"]').length).toBe(2);

    // Second Concept set in concept set list has domain "Measurement"
    const measurement_concept = wrapper.find('[data-test-id="concept-set-list-item"]').at(1)
        .find('input').first();
    measurement_concept.simulate('change');
    await waitOneTickAndUpdate(wrapper);
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="value-list-items"]').length).toBe(5);
  });

  it('should enable save button and preview button once cohorts, concepts and values are selected',
    async() => {
      const wrapper = mount(<DataSetPage />);
      await waitOneTickAndUpdate(wrapper);
      await waitOneTickAndUpdate(wrapper);

      // Preview Button and Save Button should be disabled by default
      const saveButton = wrapper.find(Button).find('[data-test-id="save-button"]')
        .first();
      expect(saveButton.prop('disabled')).toBeTruthy();
      const previewButton = wrapper.find(Clickable).find('[data-test-id="preview-button"]')
        .first();
      expect(previewButton.prop('disabled')).toBeTruthy();

      // After all cohort concept and values are selected all the buttons will be enabled

      wrapper.find('[data-test-id="cohort-list-item"]').first()
        .find('input').first().simulate('change');
      wrapper.update();

      wrapper.find('[data-test-id="concept-set-list-item"]').first()
        .find('input').first().simulate('change');

      await waitOneTickAndUpdate(wrapper);

      wrapper.find('[data-test-id="value-list-items"]').find('input').first()
        .simulate('change');

      // Buttons should now be enabled
      const buttons = wrapper.find(Button);
      expect(buttons.find('[data-test-id="save-button"]').first().prop('disabled'))
        .toBeFalsy();
      expect(wrapper.find(Clickable).find('[data-test-id="preview-button"]').first()
        .prop('disabled')).toBeFalsy();
    });

  it('should display preview data table once preview button is clicked', async() => {
    const spy = jest.spyOn(dataSetApi(), 'generateDataSetPreview');
    const wrapper = mount(<DataSetPage />);
    await waitOneTickAndUpdate(wrapper);
    await waitOneTickAndUpdate(wrapper);

    // Select one cohort , concept and value
    wrapper.find('[data-test-id="cohort-list-item"]').first()
      .find('input').first().simulate('change');
    wrapper.update();

    wrapper.find('[data-test-id="concept-set-list-item"]').first()
      .find('input').first().simulate('change');

    await waitOneTickAndUpdate(wrapper);

    wrapper.find('[data-test-id="value-list-items"]').find('input').first()
      .simulate('change');

    await waitOneTickAndUpdate(wrapper);

    // Select another value preview data api should not be called now
    wrapper.find('[data-test-id="value-list-items"]').at(1)
      .find('input').first().simulate('click');

    // Click preview button to load preview
    wrapper.find({'data-test-id': 'preview-button'}).first().simulate('click');
    await waitOneTickAndUpdate(wrapper);
    expect(spy).toHaveBeenCalledTimes(1);

  });


  it('should check that the Cohorts and Concept Sets "+" links go to their pages.', async() => {
    const wrapper = mount(<DataSetPage />);
    const pathPrefix = 'workspaces/' + workspaceDataStub.namespace + '/' + workspaceDataStub.id + '/data';

    // Mock out navigateByUrl
    const navSpy = jest.fn();
    NavStore.navigateByUrl = navSpy;

    // Check Cohorts "+" link
    wrapper.find({'data-test-id': 'cohorts-link'}).first().simulate('click');
    expect(navSpy).toHaveBeenCalledWith(pathPrefix + '/cohorts/build');

    // Check Concept Sets "+" link
    wrapper.find({'data-test-id': 'concept-sets-link'}).first().simulate('click');
    expect(navSpy).toHaveBeenCalledWith(pathPrefix + '/concepts');
  });

  it(' dataSet should show tooltip and disable SAVE button if user has READER access', async() => {
    const readWorkspace = {...workspaceStubs[0], accessLevel: WorkspaceAccessLevel.READER};
    currentWorkspaceStore.next(readWorkspace);
    const wrapper = mount(<DataSetPage />);
    const isTooltipDisable =
        wrapper.find({'data-test-id': 'save-tooltip'}).first().props().disabled;
    const isSaveButtonDisable =
        wrapper.find({'data-test-id': 'save-button'}).first().props().disabled;
    expect(isTooltipDisable).toBeFalsy();
    expect(isSaveButtonDisable).toBeTruthy();
  });

  it(' dataSet should disable cohort/concept PLUS ICON if user has READER access', async() => {
    const readWorkspace = {...workspaceStubs[0], accessLevel: WorkspaceAccessLevel.READER};
    currentWorkspaceStore.next(readWorkspace);
    const wrapper = mount(<DataSetPage />);
    const plusIconTooltip = wrapper.find({'data-test-id': 'plus-icon-tooltip'});
    const cohortplusIcon = wrapper.find({'data-test-id': 'cohorts-link'});
    const conceptSetplusIcon = wrapper.find({'data-test-id': 'concept-sets-link'});
    expect(plusIconTooltip.first().props().disabled).toBeFalsy();
    expect(cohortplusIcon.first().props().disabled).toBeTruthy();
    expect(conceptSetplusIcon.first().props().disabled).toBeTruthy();
  });
});
