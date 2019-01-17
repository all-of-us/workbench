import {shallow} from 'enzyme';
import * as React from 'react';

import {registerApiClient} from 'app/services/tsfetch';
import {ProfileApiStub} from 'testing/stubs/profile-service-stub';

import {AccountCreationResendModal, AccountCreationUpdateModal} from './component';

import {
  ProfileApi
} from 'generated/fetch';

beforeEach(() => {
  registerApiClient(ProfileApi, new ProfileApiStub());
});

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
