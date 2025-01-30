import * as React from 'react';
import { MemoryRouter } from 'react-router';
import { CompatRoute, CompatRouter } from 'react-router-dom-v5-compat';
import * as fp from 'lodash/fp';

import { ConceptSet, ConceptSetsApi, WorkspacesApi } from 'generated/fetch';

import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { dataTabPath } from 'app/routing/utils';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
import {
  currentConceptSetStore,
  currentConceptStore,
  currentWorkspaceStore,
} from 'app/utils/navigation';
import { serverConfigStore } from 'app/utils/stores';

import defaultServerConfig from 'testing/default-server-config';
import {
  expectButtonElementDisabled,
  expectSpinner,
  waitForNoSpinner,
} from 'testing/react-test-helpers';
import { ConceptSetsApiStub } from 'testing/stubs/concept-sets-api-stub';
import { workspaceDataStub } from 'testing/stubs/workspaces';
import { WorkspacesApiStub } from 'testing/stubs/workspaces-api-stub';

import { ConceptSearch } from './concept-search';
describe('ConceptSearch', () => {
  let conceptSet: ConceptSet;
  let user;

  beforeEach(() => {
    jest.clearAllMocks();
    registerApiClient(ConceptSetsApi, new ConceptSetsApiStub());
    registerApiClient(WorkspacesApi, new WorkspacesApiStub());
    currentWorkspaceStore.next(workspaceDataStub);
    currentConceptStore.next([]);
    currentConceptSetStore.next(undefined);
    serverConfigStore.set({ config: defaultServerConfig });
    conceptSet = ConceptSetsApiStub.stubConceptSets()[0];
    user = userEvent.setup();
  });

  const component = () => {
    return render(
      <MemoryRouter
        initialEntries={[
          `${dataTabPath(
            workspaceDataStub.namespace,
            workspaceDataStub.terraName
          )}/concepts/sets/${conceptSet.id}`,
        ]}
      >
        <CompatRouter>
          <CompatRoute path='/workspaces/:ns/:terraName/data/concepts/sets/:csid'>
            <ConceptSearch
              setConceptSetUpdating={() => {}}
              setShowUnsavedModal={() => {}}
              setUnsavedConceptChanges={() => {}}
              hideSpinner={() => {}}
              showSpinner={() => {}}
            />
          </CompatRoute>
        </CompatRouter>
      </MemoryRouter>
    );
  };

  it('should render', async () => {
    component();
    expectSpinner();
  });

  it('should display the participant count and domain name', async () => {
    component();
    await waitForNoSpinner();
    screen.findByText(`Participant Count: ${conceptSet.participantCount}`);
    screen.getByText(`Domain: ${fp.capitalize(conceptSet.domain.toString())}`);
  });

  it('should allow validLength edits', async () => {
    component();
    const newName = 'cool new name';
    const newDesc = 'cool new description';
    expect(await screen.findByText(conceptSet.name));
    expect(screen.getByText(conceptSet.description));
    await user.click(
      screen.getByRole('button', { name: /edit concept set button/i })
    );
    const title = screen.getByDisplayValue(conceptSet.name);
    await user.clear(title);
    await user.click(title);
    await user.paste(newName);
    const description = screen.getByText(conceptSet.description);
    await user.clear(description);
    await user.click(description);
    await user.paste(newDesc);
    await user.click(screen.getByRole('button', { name: /save/i }));
    expect(screen.getByText(newName)).toBeInTheDocument();
    expect(screen.getByText(newDesc)).toBeInTheDocument();
  });

  it('should disallow empty name edit', async () => {
    component();
    await user.click(
      await screen.findByRole('button', { name: /edit concept set button/i })
    );
    const title = screen.getByDisplayValue(conceptSet.name);
    await user.clear(title);
    await user.click(title);
    await user.paste('');
    expectButtonElementDisabled(screen.getByRole('button', { name: /save/i }));
  });

  it('should not edit on cancel', async () => {
    component();
    const newName = 'cool new name';
    const newDesc = 'cool new description';
    expect(await screen.findByText(conceptSet.name));
    expect(screen.getByText(conceptSet.description));
    await user.click(
      screen.getByRole('button', { name: /edit concept set button/i })
    );
    const title = screen.getByDisplayValue(conceptSet.name);
    await user.clear(title);
    await user.click(title);
    await user.paste(newName);
    const description = screen.getByText(conceptSet.description);
    await user.clear(description);
    await user.click(description);
    await user.paste(newDesc);
    await user.click(screen.getByRole('button', { name: /cancel/i }));
    expect(screen.getByText(conceptSet.name)).toBeInTheDocument();
    expect(screen.getByText(conceptSet.description)).toBeInTheDocument();
    expect(screen.queryByText(newName)).not.toBeInTheDocument();
    expect(screen.queryByText(newDesc)).not.toBeInTheDocument();
  });

  // TODO RW-2625: test edit and delete set from popup trigger menu
});
