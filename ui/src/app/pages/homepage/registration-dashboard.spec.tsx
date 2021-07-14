import {mount} from 'enzyme';
import * as React from 'react';

import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {profileStore, serverConfigStore} from 'app/utils/stores';
import {getTwoFactorSetupUrl, RegistrationDashboard, RegistrationDashboardProps} from 'app/pages/homepage/registration-dashboard';
import {ProfileApi} from 'generated/fetch';
import {ProfileApiStub} from 'testing/stubs/profile-api-stub';
import {buildRasRedirectUrl} from 'app/utils/ras';
import {profileApi} from 'app/services/swagger-fetch-clients';

describe('RegistrationDashboard', () => {
  let props: RegistrationDashboardProps;

  const component = () => {
    return mount<RegistrationDashboard, RegistrationDashboardProps, {trainingWarningOpen: boolean}>
    (<RegistrationDashboard {...props}/>);
  };

  beforeEach(async() => {
    registerApiClient(ProfileApi, new ProfileApiStub());
    profileStore.set({
      profile: await profileApi().getMe(),
      load: jest.fn(),
      reload: jest.fn(),
      updateCache: jest.fn()
    });
    serverConfigStore.set({config: {
      gsuiteDomain: 'fake-research-aou.org',
      projectId: 'aaa',
      publicApiKeyForErrorReports: 'aaa',
      enableEraCommons: true,
      enableRasLoginGovLinking: false,
    }});
    props = {
      eraCommonsLinked: false,
      eraCommonsLoading: false,
      eraCommonsError: '',
      rasLoginGovLinked: false,
      rasLoginGovLoading: false,
      rasLoginGovLinkError: '',
      trainingCompleted: false,
      firstVisitTraining: true,
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

  it('should display a success message when all tasks have been completed', () => {
    props.eraCommonsLinked = true;
    props.trainingCompleted = true;
    props.twoFactorAuthCompleted = true;
    props.dataUserCodeOfConductCompleted = true;
    const wrapper = component();
    expect(wrapper.find('[data-test-id="success-message"]').length).toBe(1);
  });

  it('should have RAS link card then display a success message after linking when enableRasLoginGovLinking is true', () => {
    serverConfigStore.set({config: {...serverConfigStore.get().config, enableRasLoginGovLinking: true}});
    // When enableRasLoginGovLinking is true, show RAS linking card.
    props.eraCommonsLinked = true;
    props.trainingCompleted = true;
    props.twoFactorAuthCompleted = true;
    props.dataUserCodeOfConductCompleted = true;
    props.rasLoginGovLinked = false;

    let wrapper = component();
    expect(wrapper.find('[data-test-id="success-message"]').length).toBe(0);
    expect(wrapper.find('[data-test-id="registration-task-rasLoginGov"]')
    .find('[data-test-id="registration-task-link"]').first().prop('disabled')).toBeFalsy();

    // Now mark loginGov link succeed
    props.rasLoginGovLinked = true;
    wrapper = component();
    expect(wrapper.find('[data-test-id="registration-task-rasLoginGov"]')
    .find('[data-test-id="completed-button"]').length).toBeGreaterThanOrEqual(1);
  });


  it('should not show self-bypass UI when unsafeSelfBypass is false', () => {
    serverConfigStore.set({config: {...serverConfigStore.get().config, unsafeAllowSelfBypass: false}});
    const wrapper = component();
    expect(wrapper.find('[data-test-id="self-bypass"]').length).toBe(0);
  });

  it('should show self-bypass when unsafeSelfBypass is true', () => {
    serverConfigStore.set({config: {...serverConfigStore.get().config, unsafeAllowSelfBypass: true}});
    const wrapper = component();
    expect(wrapper.find('[data-test-id="self-bypass"]').length).toBe(1);
  });

  it('should generate expected 2FA redirect URL', () => {
    expect(getTwoFactorSetupUrl()).toMatch(/https:\/\/accounts\.google\.com\/AccountChooser/);
    expect(getTwoFactorSetupUrl()).toMatch(encodeURIComponent('tester@fake-research-aou.org'));
    expect(getTwoFactorSetupUrl()).toMatch(encodeURIComponent('https://myaccount.google.com/signinoptions/'));
  });

  it('should generate expected RAS redirect URL', () => {
    expect(buildRasRedirectUrl()).toMatch(encodeURIComponent('http://localhost/ras-callback'));
  });
});
