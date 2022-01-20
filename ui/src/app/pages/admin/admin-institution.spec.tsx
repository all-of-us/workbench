import * as React from 'react';
import { mount } from 'enzyme';

import { registerApiClient } from 'app/services/swagger-fetch-clients';
import { serverConfigStore } from 'app/utils/stores';
import { InstitutionApi } from 'generated/fetch';
import defaultServerConfig from 'testing/default-server-config';
import { waitOneTickAndUpdate } from 'testing/react-test-helpers';
import { InstitutionApiStub } from 'testing/stubs/institution-api-stub';
import { AdminInstitution } from './admin-institution';

describe('AdminInstitutionSpec', () => {
  const component = () => {
    return mount(
      <AdminInstitution hideSpinner={() => {}} showSpinner={() => {}} />
    );
  };

  beforeEach(() => {
    serverConfigStore.set({ config: defaultServerConfig });

    registerApiClient(InstitutionApi, new InstitutionApiStub());
  });

  it('should render', async () => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper).toBeTruthy();
  });

  it('should display all institution', async () => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('tbody').first().find('tr').length).toBe(4);
  });
});
