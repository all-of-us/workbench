import '@testing-library/jest-dom';

import * as React from 'react';

import { ConceptSetsApi, WorkspacesApi } from 'generated/fetch';

import { render, screen } from '@testing-library/react';
import { ConceptNavigationBar } from 'app/pages/data/concept/concept-navigation-bar';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
import { currentWorkspaceStore } from 'app/utils/navigation';

import { ConceptSetsApiStub } from 'testing/stubs/concept-sets-api-stub';
import { workspaceDataStub } from 'testing/stubs/workspaces';
import { WorkspacesApiStub } from 'testing/stubs/workspaces-api-stub';

describe(ConceptNavigationBar.name, () => {
  const component = () => {
    return render(
      <ConceptNavigationBar ns='test' terraName='1' showConcepts={true} />
    );
  };

  beforeEach(() => {
    registerApiClient(WorkspacesApi, new WorkspacesApiStub());
    registerApiClient(ConceptSetsApi, new ConceptSetsApiStub());
    currentWorkspaceStore.next(workspaceDataStub);
  });

  it('should render', () => {
    component();
    expect(
      screen.getByRole('button', { name: /concepts/i })
    ).toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: /concept sets/i })
    ).toBeInTheDocument();
  });
});
