import {mount} from 'enzyme';
import * as React from 'react';

import InvitationKeyReact, {InvitationKeyProps} from './component';

import {AlertDanger} from 'app/components/alert';

import {Button} from 'app/components/buttons';
import {FormInput} from 'app/components/inputs';

describe('InvitationKeyComponent', () => {
  let props: InvitationKeyProps;
  const onInvitationVerify = jest.fn();


  const component = () => {
    return mount<InvitationKeyReact>
    (<InvitationKeyReact {...props}/>);
  };

  beforeEach(() => {
    props = {onInvitationKeyVerify: onInvitationVerify};
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
    window.fetch = jest.fn().mockImplementation(() =>
        Promise.resolve({status: 400}));

    const wrapper = component();
    const input = wrapper.find(FormInput);
    const nextButton = wrapper.find(Button);

    input.simulate('change', {target: {value: 'notValid '}});

    await nextButton.simulate('click');
    wrapper.update();
    const error = wrapper.find(AlertDanger);
    expect(error.text()).toBe('Invitation Key is not Valid.');
  });

  it('should call props onInvitationKeyVerify function on entering correct invitation key',
      async() => {
    window.fetch = jest.fn().mockImplementation(() =>
        Promise.resolve({status: 200}));

    const wrapper = component();
    const input = wrapper.find(FormInput);
    const nextButton = wrapper.find(Button);

    input.simulate('change', {target: {value: 'Correct Invitation Key'}});

    await nextButton.simulate('click');
    wrapper.update();
    expect(onInvitationVerify).toHaveBeenCalled();
  });
});
