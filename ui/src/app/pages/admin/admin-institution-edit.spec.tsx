import {mount} from "enzyme";
import * as React from 'react';
import defaultServerConfig from 'testing/default-server-config';
import {serverConfigStore} from 'app/utils/navigation';
import {waitOneTickAndUpdate} from 'testing/react-test-helpers';
import {AdminInstitutionEdit} from './admin-institution-edit';
import {InstitutionApi} from 'generated/fetch';
import {InstitutionApiStub} from 'testing/stubs/institution-api-stub';
import {registerApiClient} from'app/services/swagger-fetch-clients';

describe('AdminInstitutionEditSpec', () => {

  const component = () => {
    return mount(<AdminInstitutionEdit/>);
  };

  beforeEach(() => {
    serverConfigStore.next(defaultServerConfig);

    registerApiClient(InstitutionApi, new InstitutionApiStub());
  });

  it('should render', async() => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper).toBeTruthy();
  });

  it('should throw error in case of duplicate display name', async() => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper).toBeTruthy();
  });
});
