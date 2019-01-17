import {shallow} from 'enzyme';
import * as React from 'react';

import {AccountCreationResendModal, AccountCreationUpdateModal} from './component';

describe('AccountCreationResendModal', () => {
  it('should render', () => {
    const wrapper = shallow(<AccountCreationResendModal
      username='a'
      creationNonce='b'
      onClose={() => {}}
    />);
    expect(wrapper.exists()).toBeTruthy();
  });
});

describe('AccountCreationUpdateModal', () => {
  it('should render', () => {
    const wrapper = shallow(<AccountCreationUpdateModal
      username='a'
      creationNonce='b'
      onDone={() => {}}
      onClose={() => {}}
    />);
    expect(wrapper.exists()).toBeTruthy();
  });
});
