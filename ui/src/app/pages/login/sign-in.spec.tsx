import {Button} from 'app/components/buttons';

import {mount, shallow} from 'enzyme';
import * as React from 'react';

import {AccountCreation} from 'app/pages/login/account-creation/account-creation';
import {AccountCreationInstitution} from 'app/pages/login/account-creation/account-creation-institution';
import {AccountCreationSuccess} from 'app/pages/login/account-creation/account-creation-success';
import {AccountCreationSurvey} from 'app/pages/login/account-creation/account-creation-survey';
import {AccountCreationTos} from 'app/pages/login/account-creation/account-creation-tos';
import LoginReactComponent from 'app/pages/login/login';
import {createEmptyProfile, SignInProps, SignIn} from './sign-in';

describe('SignInReact', () => {
  let props: SignInProps;

  const signIn = jest.fn();

  const component = () => mount(<SignIn {...props}/>);

  // To correctly shallow-render this component wrapped by a HOC, we need to add an extra
  // .shallow() call at the end.
  const shallowComponent = () => shallow(<SignIn {...props}/>).shallow();

  const defaultConfig = {
    gsuiteDomain: 'researchallofus.org',
  };

  beforeEach(() => {
    window.scrollTo = () => {};
    props = {
      onSignIn: () => {},
      signIn: signIn,
      windowSize: {width: 1700, height: 0},
      hideSpinner: () => {},
      showSpinner: () => {}
    };
  });

  it('should display login background image and directive by default', () => {
    const wrapper = component();
    const templateImage = wrapper.find('[data-test-id="sign-in-page"]').hostNodes();
    const backgroundImage = templateImage.prop('style').backgroundImage;
    expect(backgroundImage).toBe('url(\'' + '/assets/images/login-group.png' + '\')');
    expect(wrapper.exists('[data-test-id="login"]')).toBeTruthy();
  });

  it('should display small background image when window width is moderately sized', () => {
    props.windowSize.width = 999;
    const wrapper = component();
    const templateImage = wrapper.find('[data-test-id="sign-in-page"]').hostNodes();
    const backgroundImage = templateImage.prop('style').backgroundImage;

    expect(backgroundImage)
      .toBe('url(\'' + '/assets/images/login-standing.png' + '\')');
    expect(wrapper.exists('[data-test-id="login"]')).toBeTruthy();
  });

  it('should display invitation key with small image when width is moderately sized ', () => {
    props.windowSize.width = 999;
    const wrapper = component();
    const createAccountButton = wrapper.find(Button).find({type: 'secondary'});
    createAccountButton.simulate('click');
    wrapper.update();
    const templateImage = wrapper.find('[data-test-id="sign-in-page"]');
  });

  it('should handle sign-up flow', async () => {
    // This test is meant to validate the high-level flow through the sign-in component by checking
    // that each step of the user registration flow is correctly rendered in order.
    //
    // As this is simply a high-level test of this component's ability to render each sub-component,
    // we use Enzyme's shallow rendering to avoid needing to deal with the DOM-level details of
    // each of the sub-components. Tests within the 'account-creation' folder should cover those
    // details.
    const wrapper = shallowComponent();

    // To start, the landing page / login component should be shown.
    expect(wrapper.exists(LoginReactComponent)).toBeTruthy();
    // Simulate the "create account" button being clicked by firing the callback method.
    wrapper.find(LoginReactComponent).props().onCreateAccount();
    await wrapper.update();

    // Terms of Service is part of the new-style flow.
    expect(wrapper.exists(AccountCreationTos)).toBeTruthy();
    wrapper.find(AccountCreationTos).props().onComplete();

    expect(wrapper.exists(AccountCreationInstitution)).toBeTruthy();
    wrapper.find(AccountCreationInstitution).props().onComplete(createEmptyProfile());

    expect(wrapper.exists(AccountCreation)).toBeTruthy();
    wrapper.find(AccountCreation).props().onComplete(createEmptyProfile());

    // Account Creation Survey (e.g. demographics) is part of the new-style flow.
    expect(wrapper.exists(AccountCreationSurvey)).toBeTruthy();
    wrapper.find(AccountCreationSurvey).props().onComplete(createEmptyProfile());

    expect(wrapper.exists(AccountCreationSuccess)).toBeTruthy();
  });
});
