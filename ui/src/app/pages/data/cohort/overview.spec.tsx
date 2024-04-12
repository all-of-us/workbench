import '@testing-library/jest-dom';

import * as React from 'react';

import { Cohort } from 'generated/fetch';

import { renderWithRouter } from '../../../../testing/react-test-helpers';
import { exampleCohortStubs } from '../../../../testing/stubs/cohorts-api-stub';
import { screen } from '@testing-library/react';

import { workspaceDataStub } from 'testing/stubs/workspaces';

import { ListOverview } from './overview';

describe('ListOverview', () => {
  const searchRequestStub = { includes: [], excludes: [], dataFilters: [] };
  it('should render', () => {
    renderWithRouter(
      <ListOverview
        searchRequest={searchRequestStub}
        update={0}
        cohort={exampleCohortStubs[0]}
        workspace={workspaceDataStub}
      />
    );
    expect(
      screen.getByRole('button', {
        name: /save cohort/i,
      })
    ).toBeInTheDocument();
  });
});
