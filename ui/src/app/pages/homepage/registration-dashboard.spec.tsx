import {mount} from 'enzyme';
import * as React from 'react';

import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {serverConfigStore} from 'app/utils/navigation';
import {
  getTwoFactorSetupUrl,
  RegistrationDashboard,
  RegistrationDashboardProps
} from 'app/pages/homepage/registration-dashboard';
import {ProfileApi} from 'generated/fetch';
import {ProfileApiStub} from 'testing/stubs/profile-api-stub';
import {userProfileStore} from 'app/utils/navigation';
import {profileApi} from 'app/services/swagger-fetch-clients';

describe('RegistrationDashboard', () => {
  let props: RegistrationDashboardProps;

  const component = () => {
    return mount<RegistrationDashboard, RegistrationDashboardProps, { trainingWarningOpen: boolean }>
    (<RegistrationDashboard {...props}/>);
  };

  beforeEach(async () => {
    registerApiClient(ProfileApi, new ProfileApiStub());
    userProfileStore.next({
      profile: await profileApi().getMe(),
      reload: jest.fn(),
      updateCache: jest.fn()
    });
    serverConfigStore.next({
      enableBetaAccess: true,
      enableDataUseAgreement: true,
      gsuiteDomain: 'fake-research-aou.org',
      projectId: 'aaa',
      publicApiKeyForErrorReports: 'aaa',
      enableEraCommons: true,
      enableV3DataUserCodeOfConduct: true,
      enableRasLoginGovLinking: false,
    });
    props = {
      eraCommonsLinked: false,
      eraCommonsLoading: false,
      eraCommonsError: '',
      rasLoginGovLinked: false,
      rasLoginGovLoading: false,
      rasLoginGovLinkError: '',
      trainingCompleted: false,
      firstVisitTraining: true,
      betaAccessGranted: false,
      twoFactorAuthCompleted: false,
      dataUserCodeOfConductCompleted: false
    };
  });

  it('should render', () => {
    const wrapper = component();
    expect(wrapper.exists()).toBeTruthy();
  });

  it('should show an error if passed an error message', () => {
    const errorMessage = 'error message!';
    props.eraCommonsError = errorMessage;
    const wrapper = component();
    expect(wrapper.find('[data-test-id="era-commons-error"]').first().text())
    .toContain(errorMessage);
  });

  it('should sequentially enable tasks', () => {
    let wrapper = component();

    // initially, first tile should be enabled and second tile should be disabled
    expect(wrapper.find('[data-test-id="registration-task-twoFactorAuth"]')
    .find('[data-test-id="registration-task-link"]').first().prop('disabled')).toBeFalsy();
    expect(wrapper.find('[data-test-id="registration-task-eraCommons"]')
    .find('[data-test-id="registration-task-link"]').first().prop('disabled')).toBeTruthy();

    props.twoFactorAuthCompleted = true;
    wrapper = component();
    // now, first tile should be disabled but completed and second tile should be enabled
    expect(wrapper.find('[data-test-id="registration-task-twoFactorAuth"]')
    .find('[data-test-id="completed-button"]').length).toBeGreaterThanOrEqual(1);
    expect(wrapper.find('[data-test-id="registration-task-eraCommons"]')
    .find('[data-test-id="registration-task-link"]').first().prop('disabled')).toBeFalsy();

  });

  it('should display a warning when beta access has not been granted', () => {
    serverConfigStore.next({...serverConfigStore.getValue(), enableBetaAccess: true});
    props.betaAccessGranted = false;
    const wrapper = component();
    expect(wrapper.find('[data-test-id="beta-access-warning"]').length).toBe(1);
  });

  it('should clear warning when user has been granted beta access', () => {
    serverConfigStore.next({...serverConfigStore.getValue(), enableBetaAccess: true});
    props.betaAccessGranted = true;
    const wrapper = component();
    expect(wrapper.find('[data-test-id="beta-access-warning"]').length).toBe(0);
  });

  it('should not display a warning when enableBetaAccess is false', () => {
    serverConfigStore.next({...serverConfigStore.getValue(), enableBetaAccess: false});
    props.betaAccessGranted = false;
    const wrapper = component();
    expect(wrapper.find('[data-test-id="beta-access-warning"]').length).toBe(0);
  });

  it('should display a success message when all tasks have been completed', () => {
    props.betaAccessGranted = true;
    props.eraCommonsLinked = true;
    props.trainingCompleted = true;
    props.twoFactorAuthCompleted = true;
    props.dataUserCodeOfConductCompleted = true;
    const wrapper = component();
    expect(wrapper.find('[data-test-id="success-message"]').length).toBe(1);
  });

  it('should display a success message when complete and enableBetaAccess is false', () => {
    serverConfigStore.next({...serverConfigStore.getValue(), enableBetaAccess: false});
    // When enableBetaAccess is false, we shouldn't need to have been granted beta access.
    props.betaAccessGranted = false;
    props.eraCommonsLinked = true;
    props.trainingCompleted = true;
    props.twoFactorAuthCompleted = true;
    props.dataUserCodeOfConductCompleted = true;
    const wrapper = component();
    expect(wrapper.find('[data-test-id="success-message"]').length).toBe(1);
  });

  it('should display a success message when complete and enableRasLoginGovLinking is true', () => {
    serverConfigStore.next({...serverConfigStore.getValue(), enableRasLoginGovLinking: true});
    // When enableBetaAccess is false, we shouldn't need to have been granted beta access.
    props.betaAccessGranted = true;
    props.eraCommonsLinked = true;
    props.trainingCompleted = true;
    props.twoFactorAuthCompleted = true;
    props.dataUserCodeOfConductCompleted = true;
    
    const wrapper = component();
    expect(wrapper.find('[data-test-id="success-message"]').length).toBe(1);
  });

  it('should not show self-bypass UI when unsafeSelfBypass is false', () => {
    serverConfigStore.next({...serverConfigStore.getValue(), unsafeAllowSelfBypass: false});
    const wrapper = component();
    expect(wrapper.find('[data-test-id="self-bypass"]').length).toBe(0);
  });

  it('should show self-bypass when unsafeSelfBypass is true', () => {
    serverConfigStore.next({...serverConfigStore.getValue(), unsafeAllowSelfBypass: true});
    const wrapper = component();
    expect(wrapper.find('[data-test-id="self-bypass"]').length).toBe(1);
  });

  it('should generate expected 2FA redirect URL', () => {
    expect(getTwoFactorSetupUrl()).toMatch(/https:\/\/accounts\.google\.com\/AccountChooser/);
    expect(getTwoFactorSetupUrl()).toMatch(encodeURIComponent('tester@fake-research-aou.org'));
    expect(getTwoFactorSetupUrl()).toMatch(encodeURIComponent('https://myaccount.google.com/signinoptions/'));
  });

});
