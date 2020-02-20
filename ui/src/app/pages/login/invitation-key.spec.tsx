import {mount} from 'enzyme';
import * as React from 'react';

import InvitationKeyReact, {InvitationKeyProps} from './invitation-key';

import {AlertDanger} from 'app/components/alert';
import {Button} from 'app/components/buttons';
import {TextInput} from 'app/components/inputs';

import {profileApi, registerApiClient} from 'app/services/swagger-fetch-clients';
import {ProfileApi} from 'generated/fetch';
import {ProfileApiStub} from 'testing/stubs/profile-api-stub';

describe('InvitationKeyComponent', () => {
  let props: InvitationKeyProps;
  const onInvitationKeyVerified = jest.fn();

  const component = () => mount(<InvitationKeyReact {...props}/>);

  beforeEach(() => {
    props = {onInvitationKeyVerified};
    registerApiClient(ProfileApi, new ProfileApiStub());
    onInvitationKeyVerified.mockClear();
  });

  it('should display required error message if Invitation key is blank', () => {
    const wrapper = component();
    const nextButton = wrapper.find(Button);
    nextButton.simulate('click');
    const error = wrapper.find(AlertDanger);
    expect(error.text()).toBe('Invitation Key is required.');
  });

  it('should display error message if Invitation key is not valid', async() => {
    const wrapper = component();
    const input = wrapper.find(TextInput);
    const nextButton = wrapper.find(Button);

    input.simulate('change', {target: {value: 'incorrect'}});
    nextButton.simulate('click');
    await new Promise(setImmediate).then(() => wrapper.update());
    const error = wrapper.find(AlertDanger);
    expect(error.text()).toBe('Invitation Key is not Valid.');
  });

  it('should call props onInvitationKeyVerify function on entering correct invitation key',
    async() => {
      const wrapper = component();
      const input = wrapper.find(TextInput);
      const nextButton = wrapper.find(Button);

      input.simulate('change', {target: {value: 'dummy'}});

      await nextButton.simulate('click');
      wrapper.update();
      expect(onInvitationKeyVerified).toHaveBeenCalled();
    });
});
