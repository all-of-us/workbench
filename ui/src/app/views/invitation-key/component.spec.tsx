import {mount} from 'enzyme';
import * as React from 'react';

import InvitationKeyReact, {InvitationKeyProps} from './component';

import {AlertDanger} from 'app/components/alert';
import {Button} from 'app/components/buttons';
import {FormInput} from 'app/components/inputs';
import {profileApi, registerApiClient} from 'app/services/swagger-fetch-clients';
import {ProfileApi} from 'generated/fetch';
import {ProfileApiStub} from 'testing/stubs/profile-api-stub';

describe('InvitationKeyComponent', () => {
  let props: InvitationKeyProps;
  const onInvitationVerify = jest.fn();


  const component = () => {
    return mount<InvitationKeyReact>
    (<InvitationKeyReact {...props}/>);
  };

  beforeEach(() => {
    props = {onInvitationKeyVerify: onInvitationVerify};
    registerApiClient(ProfileApi, new ProfileApiStub());
    onInvitationVerify.mockClear();
  });

  it('should display required error message if Invitation key is blank', () => {
    const wrapper = component();
    const nextButton = wrapper.find(Button);
    nextButton.simulate('click');
    const error = wrapper.find(AlertDanger);
    expect(error.text()).toBe('Invitation Key is required.');
  });

  it('should display error message if Invitation key is not valid', async() => {
    profileApi().invitationKeyVerification = jest.fn().mockRejectedValue(() => {
          throw new Error('test error inside');
    });

    const wrapper = component();
    const input = wrapper.find(FormInput);
    const nextButton = wrapper.find(Button);

    input.simulate('change', {target: {value: 'notValid '}});
    nextButton.simulate('click');
    await new Promise(setImmediate).then(() => wrapper.update());
    const error = wrapper.find(AlertDanger);
    expect(error.text()).toBe('Invitation Key is not Valid.');
  });

  it('should call props onInvitationKeyVerify function on entering correct invitation key',
      async() => {
        profileApi()
            .invitationKeyVerification = jest.fn()
            .mockReturnValue(Promise.resolve('result1'));


        const wrapper = component();
        const input = wrapper.find(FormInput);
        const nextButton = wrapper.find(Button);

        input.simulate('change', {target: {value: 'Correct Invitation Key'}});

        await nextButton.simulate('click');
        wrapper.update();
        expect(onInvitationVerify).toHaveBeenCalled();
      });
});
