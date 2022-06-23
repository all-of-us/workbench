import * as React from 'react';
import { MemoryRouter } from 'react-router-dom';
import { mount, shallow } from 'enzyme';

import { Button } from 'app/components/buttons';
import {
  LATEST_TOS_VERSION,
  TermsOfService,
} from 'app/components/terms-of-service';
import { AccountCreation } from 'app/pages/login/account-creation/account-creation';
import { AccountCreationInstitution } from 'app/pages/login/account-creation/account-creation-institution';
import { AccountCreationSuccess } from 'app/pages/login/account-creation/account-creation-success';
import { AccountCreationSurvey } from 'app/pages/login/account-creation/account-creation-survey';
import LoginReactComponent from 'app/pages/login/login';
import { serverConfigStore } from 'app/utils/stores';

import defaultServerConfig from 'testing/default-server-config';

import { createEmptyProfile, SignIn, SignInImpl, SignInProps } from './sign-in';

describe('SignIn', () => {
  let props: SignInProps;

  const component = () =>
    mount(
      <MemoryRouter>
        <SignIn {...props} />
      </MemoryRouter>
    );

  const shallowComponent = () => shallow(<SignInImpl {...props} />);

  beforeEach(() => {
    window.scrollTo = () => {};
    props = {
      windowSize: { width: 1700, height: 0 },
      hideSpinner: () => {},
      showSpinner: () => {},
    };
    serverConfigStore.set({
      config: {
        ...defaultServerConfig,
      },
    });
  });

  it('should display login background image and directive by default', () => {
    const wrapper = component();
    const templateImage = wrapper
      .find('[data-test-id="sign-in-page"]')
      .hostNodes();
    const backgroundImage = templateImage.prop('style').backgroundImage;
    expect(backgroundImage).toBe("url('" + 'login-group.png' + "')");
    expect(wrapper.exists('[data-test-id="login"]')).toBeTruthy();
  });

  it('should display small background image when window width is moderately sized', () => {
    props.windowSize.width = 999;
    const wrapper = component();
    const templateImage = wrapper
      .find('[data-test-id="sign-in-page"]')
      .hostNodes();
    const backgroundImage = templateImage.prop('style').backgroundImage;

    expect(backgroundImage).toBe("url('" + 'login-standing.png' + "')");
    expect(wrapper.exists('[data-test-id="login"]')).toBeTruthy();
  });

  it('should display invitation key with small image when width is moderately sized ', () => {
    props.windowSize.width = 999;
    const wrapper = component();
    const createAccountButton = wrapper
      .find(Button)
      .find({ type: 'secondary' });
    createAccountButton.simulate('click');
    wrapper.update();
    const templateImage = wrapper.find('[data-test-id="sign-in-page"]');
    expect(templateImage.exists()).toBeTruthy();
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
    expect(wrapper.exists(TermsOfService)).toBeTruthy();
    wrapper.find(TermsOfService).props().onComplete(LATEST_TOS_VERSION);

    expect(wrapper.exists(AccountCreationInstitution)).toBeTruthy();
    wrapper
      .find(AccountCreationInstitution)
      .props()
      .onComplete(createEmptyProfile());

    expect(wrapper.exists(AccountCreation)).toBeTruthy();
    wrapper.find(AccountCreation).props().onComplete(createEmptyProfile());

    // Account Creation Survey (e.g. demographics) is part of the new-style flow.
    expect(wrapper.exists(AccountCreationSurvey)).toBeTruthy();
    wrapper
      .find(AccountCreationSurvey)
      .props()
      .onComplete(createEmptyProfile());

    expect(wrapper.exists(AccountCreationSuccess)).toBeTruthy();
  });
});
