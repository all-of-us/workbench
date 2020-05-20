import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {serverConfigStore} from 'app/utils/navigation';
import {mount} from 'enzyme';
import {InstitutionApi} from 'generated/fetch';
import * as React from 'react';
import defaultServerConfig from 'testing/default-server-config';
import {waitOneTickAndUpdate} from 'testing/react-test-helpers';
import {InstitutionApiStub} from 'testing/stubs/institution-api-stub';
import {AdminInstitution} from './admin-institution';
import {InstitutionApiStub} from 'testing/stubs/institution-api-stub';
import {mount} from 'enzyme';
import {InstitutionApi} from 'generated/fetch';
import * as React from 'react';
import {AdminInstitution} from './admin-institution';
import defaultServerConfig from 'testing/default-server-config';
import {serverConfigStore} from 'app/utils/navigation';
import {waitOneTickAndUpdate} from 'testing/react-test-helpers';


describe('AdminInstitutionSpec', () => {

  const component = () => {
    return mount(<AdminInstitution/>);
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

  it('should display all institution', async() => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('tbody').first().find('tr').length).toBe(3);
  });

  it('should add link to institution name', async() => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('tbody').first().find('tr').length).toBe(3);
    expect(wrapper.find('tbody').first().find('tr').find('a').first().prop('href'))
      .toBe('admin/institution/edit/VUMC');
  });
});
