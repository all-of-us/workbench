import {mount} from 'enzyme';
import * as fp from 'lodash/fp';
import * as React from 'react';

import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {currentWorkspaceStore, urlParamsStore} from 'app/utils/navigation';
import {ConceptSet, ConceptSetsApi, WorkspacesApi} from 'generated/fetch';
import {waitOneTickAndUpdate} from 'testing/react-test-helpers';
import {ConceptSetsApiStub} from 'testing/stubs/concept-sets-api-stub';
import {workspaceDataStub, WorkspaceStubVariables} from 'testing/stubs/workspaces';
import {WorkspacesApiStub} from 'testing/stubs/workspaces-api-stub';
import {ConceptSearch} from './concept-search';

describe('ConceptSearch', () => {
  let conceptSet: ConceptSet;

  beforeEach(() => {
    registerApiClient(ConceptSetsApi, new ConceptSetsApiStub());
    registerApiClient(WorkspacesApi, new WorkspacesApiStub());
    currentWorkspaceStore.next(workspaceDataStub);
    conceptSet = ConceptSetsApiStub.stubConceptSets()[0];
    urlParamsStore.next({
      ns: WorkspaceStubVariables.DEFAULT_WORKSPACE_NS,
      wsid: WorkspaceStubVariables.DEFAULT_WORKSPACE_ID,
      csid: conceptSet.id
    });
  });

  it('should render', () => {
    const wrapper = mount(<ConceptSearch setConceptSetUpdating={() => {}}
                                         setShowUnsavedModal={() => {}}
                                         setUnsavedConceptChanges={() => {}}/>);
    expect(wrapper).toBeTruthy();
  });

  it('should display the participant count and domain name', async() => {
    const wrapper = mount(<ConceptSearch setConceptSetUpdating={() => {}}
                                         setShowUnsavedModal={() => {}}
                                         setUnsavedConceptChanges={() => {}}/>);
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="participant-count"]').text()).toContain(conceptSet.participantCount.toString());
    expect(wrapper.find('[data-test-id="concept-set-domain"]').text()).toContain(fp.capitalize(conceptSet.domain.toString()));
  });

  it('should allow validLength edits', async() => {
    const wrapper = mount(<ConceptSearch setConceptSetUpdating={() => {}}
                                         setShowUnsavedModal={() => {}}
                                         setUnsavedConceptChanges={() => {}}/>);
    const newName = 'cool new name';
    const newDesc = 'cool new description';
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="concept-set-title"]').text()).toContain(conceptSet.name);
    expect(wrapper.find('[data-test-id="concept-set-description"]').text()).toContain(conceptSet.description);
    wrapper.find('[data-test-id="edit-concept-set"]').first().simulate('click');
    await waitOneTickAndUpdate(wrapper);
    wrapper.find('[data-test-id="edit-name"]').first().simulate('change', {target: {value: newName}});
    const editDescription = wrapper.find('[id="edit-description"]');
    editDescription.find('textarea#edit-description').simulate('change', {target: {value: newDesc}});
    wrapper.find('[data-test-id="save-edit-concept-set"]').first().simulate('click');
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="concept-set-title"]').text()).toContain(newName);
    expect(wrapper.find('[data-test-id="concept-set-description"]').text()).toContain(newDesc);
  });

  it('should disallow empty name edit', async() => {
    const wrapper = mount(<ConceptSearch setConceptSetUpdating={() => {}}
                                         setShowUnsavedModal={() => {}}
                                         setUnsavedConceptChanges={() => {}}/>);
    await waitOneTickAndUpdate(wrapper);
    wrapper.find('[data-test-id="edit-concept-set"]').first().simulate('click');
    await waitOneTickAndUpdate(wrapper);
    wrapper.find('[data-test-id="edit-name"]').first()
      .simulate('change', {target: {value: ''}});
    expect(wrapper.find('[data-test-id="save-edit-concept-set"]').first()
      .props().disabled).toBeTruthy();
  });

  it('should not edit on cancel', async() => {
    const wrapper = mount(<ConceptSearch setConceptSetUpdating={() => {}}
                                         setShowUnsavedModal={() => {}}
                                         setUnsavedConceptChanges={() => {}}/>);
    const newName = 'cool new name';
    const newDesc = 'cool new description';
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="concept-set-title"]').text()).toContain(conceptSet.name);
    wrapper.find('[data-test-id="edit-concept-set"]').first().simulate('click');
    await waitOneTickAndUpdate(wrapper);
    wrapper.find('[data-test-id="edit-name"]').first().simulate('change', {target: {value: newName}});
    const editDescription = wrapper.find('[id="edit-description"]');
    editDescription.find('textarea#edit-description').simulate('change', {target: {value: newDesc}});
    wrapper.find('[data-test-id="cancel-edit-concept-set"]').first().simulate('click');
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="concept-set-title"]').text()).toContain(conceptSet.name);
  });

  // TODO RW-2625: test edit and delete set from popup trigger menu
});
