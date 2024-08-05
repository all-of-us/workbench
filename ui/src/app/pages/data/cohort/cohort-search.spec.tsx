import '@testing-library/jest-dom';

import * as React from 'react';
import { MemoryRouter } from 'react-router';

import { CohortBuilderApi, CriteriaType, Domain } from 'generated/fetch';

import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
import {
  currentCohortCriteriaStore,
  currentCohortSearchContextStore,
  currentWorkspaceStore,
} from 'app/utils/navigation';

import { renderModal } from 'testing/react-test-helpers';
import {
  CohortBuilderServiceStub,
  CriteriaStubVariables,
} from 'testing/stubs/cohort-builder-service-stub';
import { workspaceDataStub } from 'testing/stubs/workspaces';

import { CohortSearch } from './cohort-search';

const searchContextStubs = [
  {
    domain: Domain.CONDITION,
    item: {
      searchParameters: [],
    },
  },
  {
    domain: Domain.PERSON,
    item: {
      searchParameters: [],
    },
    type: CriteriaType.ETHNICITY,
  },
];

describe('CohortSearch', () => {
  const component = () => {
    renderModal(
      <MemoryRouter>
        <CohortSearch setUnsavedChanges={() => {}} />
      </MemoryRouter>
    );
  };

  beforeEach(() => {
    currentWorkspaceStore.next(workspaceDataStub);
    registerApiClient(CohortBuilderApi, new CohortBuilderServiceStub());
  });

  it('should render', async () => {
    currentCohortSearchContextStore.next(searchContextStubs[0]);
    component();
    expect(await screen.findByRole('textbox')).toBeInTheDocument();
  });

  it('should render CriteriaSearch component for any domain except Person', async () => {
    currentCohortSearchContextStore.next(searchContextStubs[0]);
    component();
    expect(
      await screen.findByRole('heading', { name: /conditions/i })
    ).toBeInTheDocument();
    expect(screen.queryByTestId('demographics')).not.toBeInTheDocument();
  });

  it('should render Demographics component for Person domain', () => {
    currentCohortSearchContextStore.next(searchContextStubs[1]);
    component();
    expect(
      screen.queryByTestId('criteria-search-container')
    ).not.toBeInTheDocument();
    expect(screen.getByTestId('demographics')).toBeInTheDocument();
  });

  it('should show warning modal for unsaved demographics selections', async () => {
    currentCohortSearchContextStore.next(searchContextStubs[1]);
    component();
    expect(
      screen.queryByTestId('cohort-search-unsaved-message')
    ).not.toBeInTheDocument();
    const selection = {
      ...CriteriaStubVariables[1],
      parameterId: 'test param id',
    };
    currentCohortCriteriaStore.next([selection]);

    const unsavedDialogText = new RegExp(
      'your cohort has not been saved\\. if you’d like to save your cohort criteria,' +
        ' please click cancel and save your changes in the right sidebar\\.',
      'i'
    );
    await waitFor(() => {
      expect(screen.queryByText(unsavedDialogText)).not.toBeInTheDocument();
    });
    const user = userEvent.setup();
    await user.click(screen.getByAltText('Go back'));
    expect(screen.queryByText(unsavedDialogText)).toBeInTheDocument();
  });
});
