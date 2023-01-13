import * as React from 'react';
import { shallow } from 'enzyme';

import { CohortBuilderApi, CriteriaType } from 'generated/fetch';

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
    const wrapper = shallow(
      <Demographics
        criteriaType={CriteriaType.GENDER}
        select={() => {}}
        selectedIds={[]}
        selections={[]}
      />
    );
    expect(wrapper).toBeTruthy();
  });
});
