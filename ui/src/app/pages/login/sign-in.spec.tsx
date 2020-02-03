import {Button} from 'app/components/buttons';

import {mount} from 'enzyme';
import * as React from 'react';

import {SignInProps, SignInReact, StepToImageConfig} from './sign-in';

describe('SignInReact', () => {
  let props: SignInProps;

  const signIn = jest.fn();

  const component = () => mount(<SignInReact {...props}/>);

  beforeEach(() => {
    props = {
      onInit: () => {},
      signIn: signIn,
      windowSize: {width: 1700, height: 0}
    } as SignInProps;
  });

  it('should display login background image and directive by default', () => {
    const wrapper = component();
    const templateImage = wrapper.find('[data-test-id="sign-in-page"]');
    const backgroundImage = templateImage.prop('style').backgroundImage;
    expect(backgroundImage).toBe('url(\'' + '/assets/images/login-group.png' + '\')');
    expect(wrapper.exists('[data-test-id="login"]')).toBeTruthy();
  });

  it('should display small background image when window width is moderately sized', () => {
    props.windowSize.width = 999;
    const wrapper = component();
    const templateImage = wrapper.find('[data-test-id="sign-in-page"]');
    const backgroundImage = templateImage.prop('style').backgroundImage;

    expect(backgroundImage)
      .toBe('url(\'' + '/assets/images/login-standing.png' + '\')');
    expect(wrapper.exists('[data-test-id="login"]')).toBeTruthy();
  });

  it('should display invitation key component on clicking Create account on login page ', () => {
    const wrapper = component();
    const createAccountButton = wrapper.find(Button).find({type: 'secondary'});
    createAccountButton.simulate('click');
    wrapper.update();
    const templateImage = wrapper.find('[data-test-id="sign-in-page"]');
    const backgroundImage = templateImage.prop('style').backgroundImage;

    expect(wrapper.exists('[data-test-id="invitationKey"]')).toBeTruthy();
  });

  it('should display invitation key with small image when width is moderately sized ', () => {
    props.windowSize.width = 999;
    const wrapper = component();
    const createAccountButton = wrapper.find(Button).find({type: 'secondary'});
    createAccountButton.simulate('click');
    wrapper.update();
    const templateImage = wrapper.find('[data-test-id="sign-in-page"]');
  });
});
