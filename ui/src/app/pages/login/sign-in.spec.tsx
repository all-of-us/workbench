import {Button} from 'app/components/buttons';

import {mount, shallow} from 'enzyme';
import * as React from 'react';

import {AccountCreation} from 'app/pages/login/account-creation/account-creation';
import {AccountCreationSuccess} from 'app/pages/login/account-creation/account-creation-success';
import {AccountCreationSurvey} from 'app/pages/login/account-creation/account-creation-survey';
import AccountCreationTos from 'app/pages/login/account-creation/account-creation-tos';
import InvitationKey from 'app/pages/login/invitation-key';
import LoginReactComponent from 'app/pages/login/login';
import {serverConfigStore} from 'app/utils/navigation';
import {createEmptyProfile, SignInProps, SignInReact, SignInReactImpl, SignInStep} from './sign-in';

describe('SignInReact', () => {
  let props: SignInProps;

  const signIn = jest.fn();

  const component = () => mount(<SignInReact {...props}/>);

  // To correctly shallow-render this component wrapped by two HOCs, we need to add two extra
  // .shallow() calls at the end.
  const shallowComponent = () => shallow(<SignInReact {...props}/>).shallow().shallow();

  const defaultConfig = {
    gsuiteDomain: 'researchallofus.org',
    enableNewAccountCreation: false,
    requireInvitationKey: true
  };

  beforeEach(() => {
    props = {
      onInit: () => {},
      signIn: signIn,
      windowSize: {width: 1700, height: 0},
      serverConfig: defaultConfig
    };
    serverConfigStore.next(defaultConfig);
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

  it('should display invitation key component on clicking Create account on login page ', () => {
    const wrapper = component();
    const createAccountButton = wrapper.find(Button).find({type: 'secondary'});
    createAccountButton.simulate('click');
    wrapper.update();
    const templateImage = wrapper.find('[data-test-id="sign-in-page"]').hostNodes();
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

  it('should handle sign-up flow for legacy account creation', () => {
    const wrapper = shallowComponent();
    const requireInstitutionalVerification = false;

    // To start, the landing page / login component should be shown.
    expect(wrapper.exists(LoginReactComponent)).toBeTruthy();
    // Simulate the "create account" button being clicked by firing the callback method.
    wrapper.find(LoginReactComponent).props().onCreateAccount();

    // Invitation key step is next.
    expect(wrapper.exists(InvitationKey)).toBeTruthy();
    wrapper.find(InvitationKey).props().onInvitationKeyVerified('asdf');

    expect(wrapper.exists(AccountCreation)).toBeTruthy();
    wrapper.find(AccountCreation).props().onComplete(createEmptyProfile(requireInstitutionalVerification));

    // Success!
    expect(wrapper.exists(AccountCreationSuccess)).toBeTruthy();
  });

  it('should handle sign-up flow for new account creation without institutional verification', () => {
    props.serverConfig = {...defaultConfig, enableNewAccountCreation: true};
    const requireInstitutionalVerification = false;

    const wrapper = shallowComponent();

    // To start, the landing page / login component should be shown.
    expect(wrapper.exists(LoginReactComponent)).toBeTruthy();
    // Simulate the "create account" button being clicked by firing the callback method.
    wrapper.find(LoginReactComponent).props().onCreateAccount();

    // Invitation key step is next.
    expect(wrapper.exists(InvitationKey)).toBeTruthy();
    wrapper.find(InvitationKey).props().onInvitationKeyVerified('asdf');

    // Terms of Service is part of the new-style flow.
    expect(wrapper.exists(AccountCreationTos)).toBeTruthy();
    wrapper.find(AccountCreationTos).props().onComplete();

    expect(wrapper.exists(AccountCreation)).toBeTruthy();
    wrapper.find(AccountCreation).props().onComplete(createEmptyProfile(requireInstitutionalVerification));

    // Account Creation Survey (e.g. demographics) is part of the new-style flow.
    expect(wrapper.exists(AccountCreationSurvey)).toBeTruthy();
    wrapper.find(AccountCreationSurvey).props().onComplete(createEmptyProfile(requireInstitutionalVerification));

    expect(wrapper.exists(AccountCreationSuccess)).toBeTruthy();
  });

  it('should handle sign-up flow for new account creation with institutional verification', () => {
    props.serverConfig = {...defaultConfig, enableNewAccountCreation: true};
    const requireInstitutionalVerification = true;

    const wrapper = shallowComponent();

    // To start, the landing page / login component should be shown.
    expect(wrapper.exists(LoginReactComponent)).toBeTruthy();
    // Simulate the "create account" button being clicked by firing the callback method.
    wrapper.find(LoginReactComponent).props().onCreateAccount();

    // Invitation key step is next.
    expect(wrapper.exists(InvitationKey)).toBeTruthy();
    wrapper.find(InvitationKey).props().onInvitationKeyVerified('asdf');

    // Terms of Service is part of the new-style flow.
    expect(wrapper.exists(AccountCreationTos)).toBeTruthy();
    wrapper.find(AccountCreationTos).props().onComplete();

    expect(wrapper.exists(AccountCreation)).toBeTruthy();
    wrapper.find(AccountCreation).props().onComplete(createEmptyProfile(requireInstitutionalVerification));

    // Account Creation Survey (e.g. demographics) is part of the new-style flow.
    expect(wrapper.exists(AccountCreationSurvey)).toBeTruthy();
    wrapper.find(AccountCreationSurvey).props().onComplete(createEmptyProfile(requireInstitutionalVerification));

    expect(wrapper.exists(AccountCreationSuccess)).toBeTruthy();
  });

  it('should skip InvitationKey when requireInvitationKey is false', () => {
    // The above two test cases do a more comprehensive check to ensure all of the prop callbacks
    // and step-incrementing logic are wired up correctly. Rather than repeating all of the above
    // boilerplate, this test focuses on the logic of creating the sign-in step order, ensuring
    // that INVITATION_KEY is removed when the flag is false.
    props.serverConfig = {...defaultConfig, requireInvitationKey: false, enableNewAccountCreation: true};

    const wrapper = shallowComponent();
    const signInImpl = wrapper.instance() as SignInReactImpl;
    const steps = signInImpl.getAccountCreationSteps();
    expect(steps).toEqual([
      SignInStep.LANDING,
      SignInStep.TERMS_OF_SERVICE,
      SignInStep.ACCOUNT_CREATION,
      SignInStep.DEMOGRAPHIC_SURVEY,
      SignInStep.SUCCESS_PAGE
    ]);
  });
});
