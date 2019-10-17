import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {shallow} from 'enzyme';

import {ProfileApi} from 'generated/fetch';
import * as React from 'react';
import {ProfileApiStub} from 'testing/stubs/profile-api-stub';

import {AccountCreationResendModal, AccountCreationUpdateModal} from './account-creation-modals';

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
