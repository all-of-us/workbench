import * as React from 'react';
import { MemoryRouter } from 'react-router-dom';
import { mount, shallow } from 'enzyme';

import { ProfileApi } from 'generated/fetch';

import { Button } from 'app/components/buttons';
import { DemographicSurvey } from 'app/components/demographic-survey-v2';
import {
  LATEST_TOS_VERSION,
  TermsOfService,
} from 'app/components/terms-of-service';
import { AccountCreation } from 'app/pages/login/account-creation/account-creation';
import { MockDate } from 'app/pages/login/account-creation/account-creation.spec';
import { AccountCreationInstitution } from 'app/pages/login/account-creation/account-creation-institution';
import { AccountCreationSuccess } from 'app/pages/login/account-creation/account-creation-success';
import LoginReactComponent from 'app/pages/login/login';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
import { serverConfigStore } from 'app/utils/stores';

import defaultServerConfig from 'testing/default-server-config';
import { ProfileApiStub } from 'testing/stubs/profile-api-stub';

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
    registerApiClient(ProfileApi, new ProfileApiStub());
    window.scrollTo = () => {};
    props = {
      windowSize: { width: 1700, height: 0 },
      hideSpinner: () => {},
      showSpinner: () => {},
      showProfileErrorModal: () => {},
    };
    serverConfigStore.set({
      config: defaultServerConfig,
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

    // the sign-up flow steps are enumerated by `SignInStep`:
    // LANDING, TERMS_OF_SERVICE, INSTITUTIONAL_AFFILIATION, ACCOUNT_DETAILS, DEMOGRAPHIC_SURVEY, SUCCESS_PAGE,

    // To start, the LANDING page / login component should be shown.
    expect(wrapper.exists(LoginReactComponent)).toBeTruthy();
    // Simulate the "create account" button being clicked by firing the callback method.
    wrapper.find(LoginReactComponent).props().onCreateAccount();
    await wrapper.update();

    // TERMS_OF_SERVICE
    expect(wrapper.exists(TermsOfService)).toBeTruthy();
    wrapper.find(TermsOfService).props().onComplete(LATEST_TOS_VERSION);

    // INSTITUTIONAL_AFFILIATION
    expect(wrapper.exists(AccountCreationInstitution)).toBeTruthy();
    wrapper
      .find(AccountCreationInstitution)
      .props()
      .onComplete(createEmptyProfile());

    // ACCOUNT_DETAILS
    expect(wrapper.exists(AccountCreation)).toBeTruthy();
    const profile = createEmptyProfile();
    profile.address.country = 'United States';
    wrapper.find(AccountCreation).props().onComplete(profile);

    // DEMOGRAPHIC_SURVEY
    expect(wrapper.exists(DemographicSurvey)).toBeTruthy();

    // final step, so hit the submit button
    const { onClick } = wrapper
      .find('[data-test-id="submit-button"]')
      .find(Button)
      .first()
      .props();

    await onClick();

    // SUCCESS_PAGE
    expect(wrapper.exists(AccountCreationSuccess)).toBeTruthy();
  });

  // TODO: Clean up after Nov 03 we do not need mock Data
  it('should not show demographic survey of international user', async () => {
    global.Date = MockDate as DateConstructor;
    const wrapper = shallowComponent();

    // the sign-up flow steps are enumerated by `SignInStep`:
    // LANDING, TERMS_OF_SERVICE, INSTITUTIONAL_AFFILIATION, ACCOUNT_DETAILS, SUCCESS_PAGE,

    // To start, the LANDING page / login component should be shown.
    expect(wrapper.exists(LoginReactComponent)).toBeTruthy();
    // Simulate the "create account" button being clicked by firing the callback method.
    wrapper.find(LoginReactComponent).props().onCreateAccount();
    await wrapper.update();

    // TERMS_OF_SERVICE
    expect(wrapper.exists(TermsOfService)).toBeTruthy();
    wrapper.find(TermsOfService).props().onComplete(LATEST_TOS_VERSION);

    // INSTITUTIONAL_AFFILIATION
    expect(wrapper.exists(AccountCreationInstitution)).toBeTruthy();
    wrapper
      .find(AccountCreationInstitution)
      .props()
      .onComplete(createEmptyProfile());

    // ACCOUNT_DETAILS
    expect(wrapper.exists(AccountCreation)).toBeTruthy();
    const profile = createEmptyProfile();
    // Updating the country to anything but US will make Account Details the last step for account creation
    profile.address.country = 'Canada';
    wrapper.find(AccountCreation).props().onComplete(profile);

    // SUCCESS_PAGE
    expect(wrapper.exists(AccountCreationSuccess)).toBeTruthy();
  });
});
