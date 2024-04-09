import '@testing-library/jest-dom';

import * as React from 'react';

import { CohortBuilderApi, CriteriaType } from 'generated/fetch';

import { render, screen } from '@testing-library/react';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
import { currentWorkspaceStore } from 'app/utils/navigation';

import { CohortBuilderServiceStub } from 'testing/stubs/cohort-builder-service-stub';
import { workspaceDataStub } from 'testing/stubs/workspaces';

import { Demographics } from './demographics';

describe('Demographics', () => {
  beforeEach(() => {
    registerApiClient(CohortBuilderApi, new CohortBuilderServiceStub());
    currentWorkspaceStore.next(workspaceDataStub);
  });

  it('should create', () => {
    render(
      <Demographics
        criteriaType={CriteriaType.GENDER}
        select={() => {}}
        selectedIds={[]}
        selections={[]}
      />
    );
    // spinner label
    expect(screen.getByLabelText('Please Wait')).toBeInTheDocument();
  });
});
