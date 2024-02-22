import '@testing-library/jest-dom';

import * as React from 'react';

import { AccessModule, ConfigResponse, ProfileApi } from 'generated/fetch';

import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Module, ModuleProps } from 'app/pages/access/module';
import { createEmptyProfile } from 'app/pages/login/sign-in';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
import { serverConfigStore } from 'app/utils/stores';

import defaultServerConfig from 'testing/default-server-config';
import { ProfileApiStub } from 'testing/stubs/profile-api-stub';

const createProps = (): ModuleProps => ({
  focused: false,
  active: true,
  eligible: true,
  moduleAction: () => {},
  moduleName: AccessModule.ERA_COMMONS,
  profile: createEmptyProfile(),
  spinnerProps: {
    showSpinner: () => {},
    hideSpinner: () => {},
  },
  status: {
    moduleName: AccessModule.ERA_COMMONS,
    completionEpochMillis: 12345,
  },
});

const setup = (
  props = createProps(),
  config: ConfigResponse = { ...defaultServerConfig }
) => {
  registerApiClient(ProfileApi, new ProfileApiStub());
  serverConfigStore.set({ config });
  return {
    container: render(<Module {...props} />).container,
    user: userEvent.setup(),
  };
};

describe(Module.name, () => {
  it('RT training should be active and clickable', () => {
    setup(
      {
        ...createProps(),
        moduleName: AccessModule.COMPLIANCE_TRAINING,
      },
      defaultServerConfig
    );
    expect(screen.queryByRole('button')).toBeInTheDocument();
  });
});
