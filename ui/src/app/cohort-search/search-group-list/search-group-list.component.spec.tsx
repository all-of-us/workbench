import {shallow} from 'enzyme';
import * as React from 'react';

import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {currentWorkspaceStore} from 'app/utils/navigation';
import {CohortBuilderApi} from 'generated/fetch';
import {CohortBuilderServiceStub} from 'testing/stubs/cohort-builder-service-stub';
import {workspaceDataStub} from 'testing/stubs/workspaces-api-stub';
import {SearchGroupList} from './search-group-list.component';

describe('SearchGroupList', () => {
  beforeEach(() => {
    registerApiClient(CohortBuilderApi, new CohortBuilderServiceStub());
    currentWorkspaceStore.next({
      ...workspaceDataStub,
      cdrVersionId: '1',
    });
  });
  it('should render', () => {
    const wrapper = shallow(<SearchGroupList role='includes' groups={[]} updateRequest={() => {}} updated={0} dataFilters={[]} />);
    expect(wrapper.exists()).toBeTruthy();
  });
});