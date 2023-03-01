import * as React from 'react';
import { shallow } from 'enzyme';

import { Domain } from 'generated/fetch';

import { SelectionList } from './selection-list';

describe('SelectionList', () => {
  it('should create', () => {
    const wrapper = shallow(
      <SelectionList
        back={() => {}}
        close={() => {}}
        disableFinish={false}
        domain={Domain.CONDITION}
        finish={() => {}}
        removeSelection={() => {}}
        selections={[]}
        setView={() => {}}
        view={''}
      />
    );
    expect(wrapper).toBeTruthy();
  });
});
