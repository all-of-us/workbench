import * as React from 'react';
import { MemoryRouter } from 'react-router';
import { mount } from 'enzyme';

import { CohortBuilderApi, CriteriaType, Domain } from 'generated/fetch';

import { registerApiClient } from 'app/services/swagger-fetch-clients';
import {
  currentCohortCriteriaStore,
  currentCohortSearchContextStore,
  currentWorkspaceStore,
} from 'app/utils/navigation';

import { waitOneTickAndUpdate } from 'testing/react-test-helpers';
import {
  CohortBuilderServiceStub,
  CriteriaStubVariables,
} from 'testing/stubs/cohort-builder-service-stub';
import { workspaceDataStub } from 'testing/stubs/workspaces';

import { CohortSearch } from './cohort-search.component';

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
    return mount(
      <MemoryRouter>
        <CohortSearch setUnsavedChanges={() => {}} />
      </MemoryRouter>
    );
  };

  beforeEach(() => {
    currentWorkspaceStore.next(workspaceDataStub);
    registerApiClient(CohortBuilderApi, new CohortBuilderServiceStub());
  });

  it('should render', () => {
    currentCohortSearchContextStore.next(searchContextStubs[0]);
    const wrapper = component();
    expect(wrapper).toBeTruthy();
  });

  it('should render CriteriaSearch component for any domain except Person', () => {
    currentCohortSearchContextStore.next(searchContextStubs[0]);
    const wrapper = component();
    expect(wrapper.find('[id="criteria-search-container"]').length).toBe(1);
    expect(wrapper.find('[data-test-id="demographics"]').length).toBe(0);
  });

  it('should render Demographics component for Person domain', () => {
    currentCohortSearchContextStore.next(searchContextStubs[1]);
    const wrapper = component();
    expect(wrapper.find('[id="criteria-search-container"]').length).toBe(0);
    expect(wrapper.find('[data-test-id="demographics"]').length).toBe(1);
  });

  it('should show warning modal for unsaved demographics selections', async () => {
    currentCohortSearchContextStore.next(searchContextStubs[1]);
    const wrapper = component();
    expect(
      wrapper.find('[data-test-id="cohort-search-unsaved-message"]').length
    ).toBe(0);
    const selection = {
      ...CriteriaStubVariables[1],
      parameterId: 'test param id',
    };
    currentCohortCriteriaStore.next([selection]);
    await waitOneTickAndUpdate(wrapper);
    wrapper.find('[data-test-id="cohort-search-back-arrow"]').simulate('click');
    expect(
      wrapper.find('[data-test-id="cohort-search-unsaved-message"]').length
    ).toBeGreaterThan(0);
  });
});
