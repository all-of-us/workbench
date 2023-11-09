import * as React from 'react';
import { MemoryRouter, Route } from 'react-router';
import * as fp from 'lodash/fp';
import { mount } from 'enzyme';

import { ConceptSet, ConceptSetsApi, WorkspacesApi } from 'generated/fetch';

import { dataTabPath, workspacePath } from 'app/routing/utils';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
import {
  currentConceptSetStore,
  currentConceptStore,
  currentWorkspaceStore,
} from 'app/utils/navigation';
import { serverConfigStore } from 'app/utils/stores';

import defaultServerConfig from 'testing/default-server-config';
import { waitOneTickAndUpdate } from 'testing/react-test-helpers';
import { ConceptSetsApiStub } from 'testing/stubs/concept-sets-api-stub';
import { workspaceDataStub } from 'testing/stubs/workspaces';
import { WorkspacesApiStub } from 'testing/stubs/workspaces-api-stub';

import { ConceptSearch } from './concept-search';

describe('ConceptSearch', () => {
  let conceptSet: ConceptSet;

  beforeEach(() => {
    jest.clearAllMocks();
    registerApiClient(ConceptSetsApi, new ConceptSetsApiStub());
    registerApiClient(WorkspacesApi, new WorkspacesApiStub());
    currentWorkspaceStore.next(workspaceDataStub);
    currentConceptStore.next([]);
    currentConceptSetStore.next(undefined);
    serverConfigStore.set({ config: defaultServerConfig });
    conceptSet = ConceptSetsApiStub.stubConceptSets()[0];
  });

  const component = () => {
    return mount(
      <MemoryRouter
        initialEntries={[
          `${dataTabPath(
            workspaceDataStub.namespace,
            workspaceDataStub.id
          )}/concepts/sets/${conceptSet.id}`,
        ]}
      >
        <Route path='/workspaces/:ns/:wsid/data/concepts/sets/:csid'>
          <ConceptSearch
            setConceptSetUpdating={() => {}}
            setShowUnsavedModal={() => {}}
            setUnsavedConceptChanges={() => {}}
            hideSpinner={() => {}}
            showSpinner={() => {}}
          />
        </Route>
      </MemoryRouter>
    );
  };

  it('should render', () => {
    const wrapper = component();
    expect(wrapper).toBeTruthy();
  });

  it('should display the participant count and domain name', async () => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="participant-count"]').text()).toContain(
      conceptSet.participantCount.toString()
    );
    expect(
      wrapper.find('[data-test-id="concept-set-domain"]').text()
    ).toContain(fp.capitalize(conceptSet.domain.toString()));
  });

  it('should allow validLength edits', async () => {
    const wrapper = component();
    const newName = 'cool new name';
    const newDesc = 'cool new description';
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="concept-set-title"]').text()).toContain(
      conceptSet.name
    );
    expect(
      wrapper.find('[data-test-id="concept-set-description"]').text()
    ).toContain(conceptSet.description);
    wrapper.find('[data-test-id="edit-concept-set"]').first().simulate('click');
    await waitOneTickAndUpdate(wrapper);
    wrapper
      .find('[data-test-id="edit-name"]')
      .first()
      .simulate('change', { target: { value: newName } });
    const editDescription = wrapper.find('[id="edit-description"]');
    editDescription
      .find('textarea#edit-description')
      .simulate('change', { target: { value: newDesc } });
    wrapper
      .find('[data-test-id="save-edit-concept-set"]')
      .first()
      .simulate('click');
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="concept-set-title"]').text()).toContain(
      newName
    );
    expect(
      wrapper.find('[data-test-id="concept-set-description"]').text()
    ).toContain(newDesc);
  });

  it('should disallow empty name edit', async () => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    wrapper.find('[data-test-id="edit-concept-set"]').first().simulate('click');
    await waitOneTickAndUpdate(wrapper);
    wrapper
      .find('[data-test-id="edit-name"]')
      .first()
      .simulate('change', { target: { value: '' } });
    expect(
      wrapper.find('[data-test-id="save-edit-concept-set"]').first().props()
        .disabled
    ).toBeTruthy();
  });

  it('should not edit on cancel', async () => {
    const wrapper = component();
    const newName = 'cool new name';
    const newDesc = 'cool new description';
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="concept-set-title"]').text()).toContain(
      conceptSet.name
    );
    wrapper.find('[data-test-id="edit-concept-set"]').first().simulate('click');
    await waitOneTickAndUpdate(wrapper);
    wrapper
      .find('[data-test-id="edit-name"]')
      .first()
      .simulate('change', { target: { value: newName } });
    const editDescription = wrapper.find('[id="edit-description"]');
    editDescription
      .find('textarea#edit-description')
      .simulate('change', { target: { value: newDesc } });
    wrapper
      .find('[data-test-id="cancel-edit-concept-set"]')
      .first()
      .simulate('click');
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="concept-set-title"]').text()).toContain(
      conceptSet.name
    );
  });

  // TODO RW-2625: test edit and delete set from popup trigger menu
});
