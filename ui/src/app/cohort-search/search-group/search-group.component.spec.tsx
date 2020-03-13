import {mount} from 'enzyme';
import * as React from 'react';

import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {currentWorkspaceStore} from 'app/utils/navigation';
import {CohortBuilderApi, DomainType} from 'generated/fetch';
import {CohortBuilderServiceStub} from 'testing/stubs/cohort-builder-service-stub';
import {workspaceDataStub} from 'testing/stubs/workspaces-api-stub';
import {SearchGroup} from './search-group.component';

const itemsStub = [
  {
    id: 'itemA',
    type: DomainType.MEASUREMENT,
    searchParameters: [],
    modifiers: [],
    count: 1,
    temporalGroup: 0,
    isRequesting: false,
    status: 'active'
  },
  {
    id: 'itemB',
    type: DomainType.MEASUREMENT,
    searchParameters: [],
    modifiers: [],
    count: 2,
    temporalGroup: 0,
    isRequesting: false,
    status: 'active'
  },
  {
    id: 'itemC',
    type: DomainType.MEASUREMENT,
    searchParameters: [],
    modifiers: [],
    count: 3,
    temporalGroup: 1,
    isRequesting: false,
    status: 'active'
  }
];
const groupStub = {id: 'group_id', items: itemsStub, status: 'active', type: DomainType.CONDITION};
describe('SearchGroup', () => {
  beforeEach(() => {
    registerApiClient(CohortBuilderApi, new CohortBuilderServiceStub());
    currentWorkspaceStore.next({
      ...workspaceDataStub,
      cdrVersionId: '1',
    });
  });

  it('should render', () => {
    const wrapper = mount(<SearchGroup role='includes' group={groupStub} index={0} updateRequest={() => {}}/>);
    expect(wrapper.exists()).toBeTruthy();
  });

  it('Should render each item in items prop', async() => {
    const wrapper = mount(<SearchGroup role='includes' group={groupStub} index={0} updateRequest={() => {}}/>);
    expect(wrapper.find('[data-test-id="item-list"]').length).toBe(itemsStub.length);
    wrapper.setProps({group: {...groupStub, temporal: true}});
    // Should split the items by temporalGroup when temporal is true
    expect(wrapper.find('[data-test-id="item-list"]').length).toBe(itemsStub.filter(it => it.temporalGroup === 0).length);
    expect(wrapper.find('[data-test-id="temporal-item-list"]').length).toBe(itemsStub.filter(it => it.temporalGroup === 1).length);
  });
});
