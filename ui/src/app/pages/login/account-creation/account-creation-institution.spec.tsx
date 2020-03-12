import {mount} from 'enzyme';
import * as React from 'react';

import {serverConfigStore} from 'app/utils/navigation';
import {ConfigApi, InstitutionApi, Profile} from 'generated/fetch';
import {createEmptyProfile} from 'app/pages/login/sign-in';
import {AccountCreationInstitution, Props} from './account-creation-institution';
import {ConfigApiStub} from 'testing/stubs/config-api-stub';
import {InstitutionApiStub} from 'testing/stubs/institution-api-stub';
import {registerApiClient} from 'app/services/swagger-fetch-clients';
import defaultServerConfig from 'testing/default-server-config';
import {institutionApi} from 'app/services/swagger-fetch-clients';
import SpyInstance = jest.SpyInstance;
import {Dropdown} from 'primereact/dropdown';
import {waitOneTickAndUpdate} from 'testing/react-test-helpers';
import {defaultInstitutions} from 'testing/stubs/institution-api-stub';

let mockGetPublicInstitutionDetails: SpyInstance;

let props: Props;
const component = () => {
  return mount(<AccountCreationInstitution {...props}/>);
};

beforeEach(() => {
  serverConfigStore.next(defaultServerConfig);
  registerApiClient(ConfigApi, new ConfigApiStub());
  registerApiClient(InstitutionApi, new InstitutionApiStub());

  props = {
    profile: createEmptyProfile(true),
    onComplete: (profile: Profile) => {},
    onPreviousClick: (profile: Profile) => {}
  };

  mockGetPublicInstitutionDetails = jest.spyOn(institutionApi(), 'getPublicInstitutionDetails');
});

it('should render', async() => {
  const wrapper = component();
  expect(wrapper.exists()).toBeTruthy();
});

it('should load institutions list', async() => {
  const wrapper = component();

  await waitOneTickAndUpdate(wrapper);
  expect(mockGetPublicInstitutionDetails).toHaveBeenCalled();

  const options = wrapper.find('Dropdown[data-test-id="institution-dropdown"]').prop('options') as Array<Object>;
  expect(options.length).toEqual(defaultInstitutions.length);
});

it('should show role list ', async() => {
  const wrapper = component();

  await waitOneTickAndUpdate(wrapper);
  expect(mockGetPublicInstitutionDetails).toHaveBeenCalled();

  wrapper.find('Dropdown[data-test-id="institution-dropdown"]')
  
});
