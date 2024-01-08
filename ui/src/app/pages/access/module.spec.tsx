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
  it('deactivates RT training during the training migration pause', () => {
    setup(
      {
        ...createProps(),
        moduleName: AccessModule.COMPLIANCE_TRAINING,
      },
      {
        ...defaultServerConfig,
        trainingMigrationPauseActive: true,
      }
    );

    // Assert the module is not clickable
    expect(screen.queryByRole('button')).not.toBeInTheDocument();
  });

  it('does not deactivate RT training when the training migration pause is not happening', () => {
    setup(
      {
        ...createProps(),
        moduleName: AccessModule.COMPLIANCE_TRAINING,
      },
      {
        ...defaultServerConfig,
        trainingMigrationPauseActive: false,
      }
    );

    // Assert the module is clickable
    expect(screen.getByRole('button')).toBeInTheDocument();
  });

  it('deactivates CT training during the training migration pause', () => {
    setup(
      {
        ...createProps(),
        moduleName: AccessModule.CT_COMPLIANCE_TRAINING,
      },
      {
        ...defaultServerConfig,
        trainingMigrationPauseActive: true,
      }
    );

    // Assert the module is not clickable
    expect(screen.queryByRole('button')).not.toBeInTheDocument();
  });

  it('does not deactivate CT training when the training migration pause is not happening', () => {
    setup(
      {
        ...createProps(),
        moduleName: AccessModule.CT_COMPLIANCE_TRAINING,
      },
      {
        ...defaultServerConfig,
        trainingMigrationPauseActive: false,
      }
    );

    // Assert the module is clickable
    expect(screen.getByRole('button')).toBeInTheDocument();
  });

  it('does not deactivate non-training modules during the training migration pause', () => {
    setup(
      {
        ...createProps(),
        // Arbitrary non-training module
        moduleName: AccessModule.ERA_COMMONS,
      },
      {
        ...defaultServerConfig,
        trainingMigrationPauseActive: true,
      }
    );

    // Assert the module is clickable
    expect(screen.getByRole('button')).toBeInTheDocument();
  });
});
