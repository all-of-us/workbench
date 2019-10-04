import {shallow} from 'enzyme';
import * as React from 'react';

import {DomainType} from 'generated/fetch';
import {SearchGroupItem} from './search-group-item.component';

const itemStub = {id: 'item_id', searchParameters: [], status: 'active', type: DomainType.CONDITION}
describe('SearchGroupItem', () => {
  it('should render', () => {
    const wrapper = shallow(<SearchGroupItem role='includes' groupId='group_id' item={itemStub} index={0} updateGroup={() => {}}/>);
    expect(wrapper.exists()).toBeTruthy();
  });
});