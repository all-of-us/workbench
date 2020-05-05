import {shallow} from 'enzyme';
import * as React from 'react';

import {DomainType} from 'generated/fetch';
import {SelectionList} from './selection-list.component';

describe('SelectionList', () => {
  it('should create', () => {
    const wrapper = shallow(<SelectionList back={() => {}}
                                           cancel={() => {}}
                                           disableFinish={false}
                                           domain={DomainType.CONDITION}
                                           finish={() => {}}
                                           removeSelection={() => {}}
                                           selections={[]}
                                           setView={() => {}}
                                           view={''}/>);
  });
});
