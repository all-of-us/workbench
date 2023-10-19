import * as React from 'react';
import { MemoryRouter } from 'react-router';
import { mount } from 'enzyme';

import {CohortBuilderApi} from 'generated/fetch';

import { VariantSearch } from 'app/pages/data/cohort/variant-search';
import {registerApiClient} from 'app/services/swagger-fetch-clients';
import { currentWorkspaceStore } from 'app/utils/navigation';

import { CohortBuilderServiceStub } from 'testing/stubs/cohort-builder-service-stub';
import { workspaceDataStub } from 'testing/stubs/workspaces';

describe('VariantSearch', () => {
  beforeEach(() => {
    currentWorkspaceStore.next(workspaceDataStub);
    registerApiClient(CohortBuilderApi, new CohortBuilderServiceStub());
  });
  const component = () => {

    return mount(
      <MemoryRouter>
        <VariantSearch select={() => {}} selectedIds={[]} />
      </MemoryRouter>
    );
  };

  it('should render', () => {
    const wrapper = component();
    expect(wrapper).toBeTruthy();
  });
});
