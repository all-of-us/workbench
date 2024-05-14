import * as React from 'react';

import { ConceptSetsApi } from 'generated/fetch';

import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
import { currentWorkspaceStore } from 'app/utils/navigation';

import {
  expectButtonElementDisabled,
  expectButtonElementEnabled,
  renderModal,
} from 'testing/react-test-helpers';
import { CardCountStubVariables } from 'testing/stubs/cohort-builder-service-stub';
import {
  ConceptSetsApiStub,
  ConceptStubVariables,
} from 'testing/stubs/concept-sets-api-stub';
import { workspaceDataStub } from 'testing/stubs/workspaces';

import { ConceptAddModal } from './concept-add-modal';

describe('ConceptAddModal', () => {
  let props;
  let user;
  let conceptSetsApi: ConceptSetsApiStub;
  const stubConcepts = ConceptStubVariables.STUB_CONCEPTS;
  const activeDomainTab = CardCountStubVariables.STUB_CARD_COUNTS[0];

  const component = () => {
    return renderModal(<ConceptAddModal {...props} />);
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
    user = userEvent.setup();
  });

  it('finds the correct number of concepts in the selected domain', async () => {
    component();
    const stubConceptsInDomain = stubConcepts.filter(
      (c) => c.domainId === activeDomainTab.domain.toString()
    );

    expect(
      (await screen.findAllByTestId('add-concept-title'))[0].textContent
    ).toBe(
      'Add ' +
        stubConceptsInDomain.length +
        ' Concepts to ' +
        activeDomainTab.name +
        ' Concept Set'
    );
  });

  it('displays option to add to existing concept set if concept set in domain exists', async () => {
    component();
    const stubSetsInDomain = conceptSetsApi.conceptSets
      .filter((s) => s.domain === activeDomainTab.domain)
      .map((s) => s.name);
    expect(await screen.findByTestId('add-to-existing')).toBeInTheDocument();
    const foundSets = (await screen.findAllByTestId('existing-set')).map(
      (s) => s.textContent
    );
    expect(foundSets).toEqual(stubSetsInDomain);
  });

  it('disables option to add to existing if concept set does not exist & defaults to create', async () => {
    props.activeDomainTab = CardCountStubVariables.STUB_CARD_COUNTS[2];
    component();
    expect(screen.queryByTestId('add-to-existing')).not.toBeInTheDocument();
    expect(await screen.findByTestId('create-new-set')).toBeInTheDocument();
    expect(screen.getByTestId('toggle-existing-set')).toBeDisabled();
  });

  it('allows user to toggle to create new set', async () => {
    component();
    expect(await screen.findByTestId('add-to-existing')).toBeInTheDocument();
    await user.click(screen.getByTestId('toggle-new-set'));
    expect(screen.getByTestId('create-new-set')).toBeInTheDocument();
  });

  it('disables save button if user enters an invalid name for a new set', async () => {
    component();
    const stubSetsInDomain = conceptSetsApi.conceptSets
      .filter((s) => s.domain === activeDomainTab.domain)
      .map((s) => s.name);
    await user.click(await screen.findByTestId('toggle-new-set'));

    // empty name cannot be saved
    expectButtonElementDisabled(screen.getByRole('button', { name: 'Save' }));

    // existing name cannot be saved
    await user.click(screen.getByTestId('create-new-set-name'));
    await user.paste(stubSetsInDomain[0]);

    expectButtonElementDisabled(screen.getByRole('button', { name: 'Save' }));

    await user.click(screen.getByTestId('create-new-set-name'));
    await user.paste('newsetname!!!');

    expectButtonElementEnabled(screen.getByRole('button', { name: 'Save' }));
  });
});
