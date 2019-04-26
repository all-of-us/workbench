import {mount} from 'enzyme';
import * as React from 'react';

import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {RegistrationDashboard, RegistrationDashboardProps} from 'app/views/registration-dashboard/component';
import {ProfileApi} from 'generated/fetch';
import {ProfileApiStub} from 'testing/stubs/profile-api-stub';

describe('RegistrationDashboard', () => {
  let props: RegistrationDashboardProps;

  const component = () => {
    return mount<RegistrationDashboard, RegistrationDashboardProps, {trainingWarningOpen: boolean}>
    (<RegistrationDashboard {...props}/>);
  };

  beforeEach(() => {
    registerApiClient(ProfileApi, new ProfileApiStub());
    props  = {
      eraCommonsLinked: false,
      eraCommonsError: '',
      trainingCompleted: false,
      firstVisitTraining: true,
      betaAccessGranted: true,
      dataUseAgreementCompleted: false
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
    expect(wrapper.find('[data-test-id="registration-task-0"]')
      .find('[data-test-id="registration-task-link"]').first().prop('disabled')).toBeFalsy();
    expect(wrapper.find('[data-test-id="registration-task-1"]')
      .find('[data-test-id="registration-task-link"]').first().prop('disabled')).toBeTruthy();

    props.trainingCompleted = true;
    wrapper = component();
    // now, first tile should be disabled but completed and second tile should be enabled
    expect(wrapper.find('[data-test-id="registration-task-0"]')
      .find('[data-test-id="completed-button"]').length).toBeGreaterThanOrEqual(1);
    expect(wrapper.find('[data-test-id="registration-task-1"]')
      .find('[data-test-id="registration-task-link"]').first().prop('disabled')).toBeFalsy();

  });

  it('should display a warning when beta access has not been granted', () => {
    props.betaAccessGranted = false;
    const wrapper = component();
    expect(wrapper.find('[data-test-id="beta-access-warning"]').length).toBe(1);
  });

  it('should display a success message when all tasks have been completed', () => {
    props.eraCommonsLinked = true;
    props.trainingCompleted = true;
    props.dataUseAgreementCompleted = true;
    const wrapper = component();
    expect(wrapper.find('[data-test-id="success-message"]').length).toBe(1);
  });

});
