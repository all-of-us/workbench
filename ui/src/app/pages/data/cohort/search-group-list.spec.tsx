import * as React from 'react';
import { shallow } from 'enzyme';

import { CohortBuilderApi } from 'generated/fetch';

import { registerApiClient } from 'app/services/swagger-fetch-clients';
import { currentWorkspaceStore } from 'app/utils/navigation';

import { CohortBuilderServiceStub } from 'testing/stubs/cohort-builder-service-stub';
import { workspaceDataStub } from 'testing/stubs/workspaces';

import { SearchGroupList } from './search-group-list';

describe('SearchGroupList', () => {
  beforeEach(() => {
    registerApiClient(CohortBuilderApi, new CohortBuilderServiceStub());
    currentWorkspaceStore.next({
      ...workspaceDataStub,
      cdrVersionId: '1',
    });
  });
  it('should render', () => {
    const wrapper = shallow(
      <SearchGroupList
        role='includes'
        groups={[]}
        setSearchContext={() => {}}
        updateRequest={() => {}}
        updated={0}
      />
    );
    expect(wrapper.exists()).toBeTruthy();
  });
});
