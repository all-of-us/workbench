import * as React from 'react';
import { mount } from 'enzyme';

import { ConceptSetsApi } from 'generated/fetch/api';

import { registerApiClient } from 'app/services/swagger-fetch-clients';
import { currentWorkspaceStore } from 'app/utils/navigation';

import { waitOneTickAndUpdate } from 'testing/react-test-helpers';
import { CardCountStubVariables } from 'testing/stubs/cohort-builder-service-stub';
import {
  ConceptSetsApiStub,
  ConceptStubVariables,
} from 'testing/stubs/concept-sets-api-stub';
import { workspaceDataStub } from 'testing/stubs/workspaces';

import { ConceptAddModal } from './concept-add-modal';

describe('ConceptAddModal', () => {
  let props;
  let conceptSetsApi: ConceptSetsApiStub;
  const stubConcepts = ConceptStubVariables.STUB_CONCEPTS;
  const activeDomainTab = CardCountStubVariables.STUB_CARD_COUNTS[0];

  const component = () => {
    return mount(<ConceptAddModal {...props} />);
  };

  beforeEach(() => {
    props = {
      onSave: () => {},
      onClose: () => {},
      selectedConcepts: stubConcepts.filter(
        (c) => c.domainId === activeDomainTab.domain.toString()
      ),
      activeDomainTab: activeDomainTab,
    };

    conceptSetsApi = new ConceptSetsApiStub();
    registerApiClient(ConceptSetsApi, conceptSetsApi);
    currentWorkspaceStore.next(workspaceDataStub);
  });

  it('finds the correct number of concepts in the selected domain', async () => {
    const wrapper = component();
    const stubConceptsInDomain = stubConcepts.filter(
      (c) => c.domainId === activeDomainTab.domain.toString()
    );
    expect(
      wrapper.find('[data-test-id="add-concept-title"]').first().text()
    ).toBe(
      'Add ' +
        stubConceptsInDomain.length +
        ' Concepts to ' +
        activeDomainTab.name +
        ' Concept Set'
    );
  });

  it('displays option to add to existing concept set if concept set in domain exists', async () => {
    const wrapper = component();
    const stubSetsInDomain = conceptSetsApi.conceptSets
      .filter((s) => s.domain === activeDomainTab.domain)
      .map((s) => s.name);
    await waitOneTickAndUpdate(wrapper);
    expect(
      wrapper.find('[data-test-id="add-to-existing"]').exists()
    ).toBeTruthy();
    const foundSets = wrapper
      .find('[data-test-id="existing-set"]')
      .map((s) => s.text());
    expect(foundSets).toEqual(stubSetsInDomain);
  });

  it('disables option to add to existing if concept set does not exist & defaults to create', async () => {
    props.activeDomainTab = CardCountStubVariables.STUB_CARD_COUNTS[2];
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(
      wrapper.find('[data-test-id="add-to-existing"]').exists()
    ).toBeFalsy();
    expect(
      wrapper.find('[data-test-id="create-new-set"]').exists()
    ).toBeTruthy();
    expect(
      wrapper
        .find('[data-test-id="toggle-existing-set"]')
        .first()
        .prop('disabled')
    ).toBe(true);
  });

  it('allows user to toggle to create new set', async () => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(
      wrapper.find('[data-test-id="add-to-existing"]').exists()
    ).toBeTruthy();
    wrapper.find('[data-test-id="toggle-new-set"]').first().simulate('click');
    expect(
      wrapper.find('[data-test-id="create-new-set"]').exists()
    ).toBeTruthy();
  });

  it('disables save button if user enters an invalid name for a new set', async () => {
    const wrapper = component();
    const stubSetsInDomain = conceptSetsApi.conceptSets
      .filter((s) => s.domain === activeDomainTab.domain)
      .map((s) => s.name);
    await waitOneTickAndUpdate(wrapper);
    wrapper.find('[data-test-id="toggle-new-set"]').first().simulate('click');

    // empty name cannot be saved
    expect(
      wrapper.find('[data-test-id="save-concept-set"]').first().prop('disabled')
    ).toBe(true);

    // existing name cannot be saved
    wrapper
      .find('[data-test-id="create-new-set-name"]')
      .find('input')
      .simulate('change', { target: { value: stubSetsInDomain[0] } });
    expect(
      wrapper.find('[data-test-id="save-concept-set"]').first().prop('disabled')
    ).toBe(true);

    wrapper
      .find('[data-test-id="create-new-set-name"]')
      .find('input')
      .simulate('change', { target: { value: 'newsetname!!!' } });
    expect(
      wrapper.find('[data-test-id="save-concept-set"]').first().prop('disabled')
    ).toBe(false);
  });
});
