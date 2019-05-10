import {mount} from 'enzyme';
import * as React from 'react';

import {Button} from 'app/components/buttons';
import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {currentWorkspaceStore, urlParamsStore} from 'app/utils/navigation';
import {DataSetPage} from 'app/views/dataset-page/component';
import {WorkspaceAccessLevel} from 'generated';
import {CohortsApi, ConceptsApi, ConceptSetsApi, DataSet} from 'generated/fetch';
import {waitOneTickAndUpdate} from 'testing/react-test-helpers';
import {CohortsApiStub, exampleCohortStubs} from 'testing/stubs/cohorts-api-stub';
import {ConceptSetsApiStub} from 'testing/stubs/concept-sets-api-stub';
import {ConceptsApiStub} from 'testing/stubs/concepts-api-stub';
import {WorkspacesServiceStub} from 'testing/stubs/workspace-service-stub';
import {WorkspaceStubVariables} from 'testing/stubs/workspaces-api-stub';

describe('DataSet', () => {
  beforeEach(() => {
    registerApiClient(CohortsApi, new CohortsApiStub());
    registerApiClient(ConceptsApi, new ConceptsApiStub());
    registerApiClient(ConceptSetsApi, new ConceptSetsApiStub());
    urlParamsStore.next({
      ns: WorkspaceStubVariables.DEFAULT_WORKSPACE_NS,
      wsid: WorkspaceStubVariables.DEFAULT_WORKSPACE_ID
    });
    currentWorkspaceStore.next({
      ...WorkspacesServiceStub.stubWorkspace(),
      accessLevel: WorkspaceAccessLevel.OWNER,
    });
  });

  it('should render', async() => {
    const wrapper = mount(<DataSetPage />);
    await waitOneTickAndUpdate(wrapper);
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.exists()).toBeTruthy();
  });

//  it should load all concept sets related to workspace
  it ('should display all concepts sets in workspace', async() => {
    const wrapper = mount(<DataSetPage />);
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="concept-set-list-item"]').length)
      .toBe(ConceptSetsApiStub.stubConceptSets().length);
  });
//  it should load all cohorts related to workspace

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
    condition_concept.simulate('click');
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="value-list-items"]').length).toBe(2);

    // Second Concept set in concept set list has domain "Measurement"
    const measurement_concept = wrapper.find('[data-test-id="concept-set-list-item"]').at(1)
        .find('input').first();
    measurement_concept.simulate('click');
    await waitOneTickAndUpdate(wrapper);
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="value-list-items"]').length).toBe(5);
  });

  it('should enable all buttons once cohorts, concepts and values are selected', async() => {
    const wrapper = mount(<DataSetPage />);
    await waitOneTickAndUpdate(wrapper);
    await waitOneTickAndUpdate(wrapper);

    // Preview Button by default should be disabled
    const previewButton = wrapper.find(Button).find('[data-test-id="preview-button"]')
        .first();
    expect(previewButton.prop('disabled')).toBeTruthy();

    // After all cohort concept and values are selected all the buttons will be enabled

    wrapper.find('[data-test-id="cohort-list-item"]').first()
      .find('input').first().simulate('click');
    wrapper.update();

    wrapper.find('[data-test-id="concept-set-list-item"]').first()
      .find('input').first().simulate('click');

    await waitOneTickAndUpdate(wrapper);

    wrapper.find('[data-test-id="value-list-items"]').find('input').first()
      .simulate('change');

    // Buttons should now be enabled
    const buttons = wrapper.find(Button);
    expect(buttons.find('[data-test-id="preview-button"]').first().prop('disabled'))
      .toBeFalsy();
    expect(buttons.find('[data-test-id="save-button"]').first().prop('disabled'))
      .toBeFalsy();
  });
});
