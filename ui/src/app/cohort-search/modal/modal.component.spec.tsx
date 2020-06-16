import {shallow} from 'enzyme';
import * as React from 'react';

import {DomainType} from 'generated/fetch';
import {CBModal} from './modal.component';

const searchContextStub = {
  domain: DomainType.CONDITION,
  item: {
    searchParameters: []
  }
};

describe('CBModal', () => {
  it('should render', () => {
    const wrapper = shallow(<CBModal closeSearch={() => {}} setSearchContext={(context) => {}} searchContext={searchContextStub}/>);
    expect(wrapper).toBeTruthy();
  });
});
