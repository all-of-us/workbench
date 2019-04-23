import {mount} from 'enzyme';
import * as React from 'react';

import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {RegistrationPage, RegistrationPageProps} from 'app/views/registration-page/component';
import {ProfileApi} from 'generated/fetch';
import {ProfileApiStub} from 'testing/stubs/profile-api-stub';

describe('RegistrationPage', () => {
  let props: RegistrationPageProps;

  const component = () => {
    return mount<RegistrationPage, RegistrationPageProps, {trainingWarningOpen: boolean}>
    (<RegistrationPage {...props}/>);
  };

  beforeEach(() => {
    registerApiClient(ProfileApi, new ProfileApiStub());
    props  = {
      eraCommonsLinked: false,
      eraCommonsError: '',
      trainingCompleted: false,
      firstVisitTraining: true,
      betaAccessGranted: true
    }
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

});