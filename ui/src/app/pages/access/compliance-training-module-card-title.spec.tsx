import '@testing-library/jest-dom';

import * as React from 'react';

import {
  AccessModule,
  ConfigResponse,
  Profile,
  ProfileApi,
} from 'generated/fetch';

import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {
  ComplianceTrainingModuleCardProps,
  ComplianceTrainingModuleCardTitle,
} from 'app/pages/access/compliance-training-module-card-title';
import {
  findCTTitle,
  findRTTitle,
  queryForCTTitle,
  queryForRTTitle,
} from 'app/pages/access/test-utils';
import { createEmptyProfile } from 'app/pages/login/sign-in';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
import { AccessTierShortNames } from 'app/utils/access-tiers';
import { nowPlusDays } from 'app/utils/dates';
import { serverConfigStore } from 'app/utils/stores';

import defaultServerConfig from 'testing/default-server-config';
import { ProfileApiStub } from 'testing/stubs/profile-api-stub';

const queryHelpText = () => {
  return screen.queryByText(
    /navigate to "my courses" and select "responsible conduct of research"/i
  );
};

const queryMaintenanceText = () => {
  return screen.queryByText(
    new RegExp(
      'our training system is conducting scheduled maintenance from ' +
        'october 15th through october 18th. please return after our ' +
        'maintenance window in order to complete your training.',
      'i'
    )
  );
};

const expectHelpTextToExist = async () => {
  expect(queryHelpText()).toBeInTheDocument();
};

const expectHelpTextToNotExist = () => {
  expect(queryHelpText()).not.toBeInTheDocument();
};

const expectMaintenanceTextToExist = () => {
  expect(queryMaintenanceText()).toBeInTheDocument();
};

const expectMaintenanceTextToNotExist = () => {
  expect(queryMaintenanceText()).not.toBeInTheDocument();
};

// Ensures that the component is rendered so that the test can check for the abcense of other text
const expectComponentToHaveRendered = async (
  accessTier: AccessTierShortNames
) => {
  await (accessTier === AccessTierShortNames.Registered
    ? findRTTitle()
    : findCTTitle());
};

const createProfileWithComplianceTraining = (
  completionEpochMillis: number | null,
  expirationEpochMillis: number | null,
  bypassEpochMillis: number | null
): Profile => ({
  ...createEmptyProfile(),
  accessModules: {
    modules: [
      {
        moduleName: AccessModule.COMPLIANCE_TRAINING,
        completionEpochMillis,
        expirationEpochMillis,
        bypassEpochMillis,
      },
    ],
  },
});

const createProps = (): ComplianceTrainingModuleCardProps => ({
  tier: AccessTierShortNames.Registered,
  profile: {
    ...createEmptyProfile(),
  },
});

const setup = (
  props = createProps(),
  config: ConfigResponse = { ...defaultServerConfig }
) => {
  registerApiClient(ProfileApi, new ProfileApiStub());
  serverConfigStore.set({ config });
  return {
    container: render(<ComplianceTrainingModuleCardTitle {...props} />)
      .container,
    user: userEvent.setup(),
  };
};

describe(ComplianceTrainingModuleCardTitle.name, () => {
  it('renders registered tier training title', () => {
    setup({
      ...createProps(),
      tier: AccessTierShortNames.Registered,
    });

    expect(queryForRTTitle()).not.toBeNull();
  });

  it('renders controlled tier training title', () => {
    setup({
      ...createProps(),
      tier: AccessTierShortNames.Controlled,
    });

    expect(queryForCTTitle()).not.toBeNull();
  });

  it('shows help text if module is incomplete', async () => {
    setup(
      {
        ...createProps(),
        tier: AccessTierShortNames.Registered,
        profile: createProfileWithComplianceTraining(null, null, null),
      },
      defaultServerConfig
    );

    expectHelpTextToExist();
    expectMaintenanceTextToNotExist();
  });

  it('shows maintenance text if module is incomplete and training is in a maintenance window', async () => {
    setup(
      {
        ...createProps(),
        tier: AccessTierShortNames.Registered,
        profile: createProfileWithComplianceTraining(null, null, null),
      },
      { ...defaultServerConfig, blockComplianceTraining: true }
    );

    expectMaintenanceTextToExist();
    expectHelpTextToNotExist();
  });

  it('shows help text if compliance training is complete and module is expiring', async () => {
    setup(
      {
        ...createProps(),
        profile: createProfileWithComplianceTraining(
          nowPlusDays(-1),
          nowPlusDays(29),
          null
        ),
      },
      {
        ...defaultServerConfig,
        complianceTrainingRenewalLookback: 30,
      }
    );

    expectHelpTextToExist();
    expectMaintenanceTextToNotExist();
  });

  it(
    'shows maintenance text if compliance training is complete and' +
      'module is expiring and training is in a maintenance window',
    async () => {
      setup(
        {
          ...createProps(),
          profile: createProfileWithComplianceTraining(
            nowPlusDays(-1),
            nowPlusDays(29),
            null
          ),
        },
        {
          ...defaultServerConfig,
          blockComplianceTraining: true,
          complianceTrainingRenewalLookback: 30,
        }
      );

      expectMaintenanceTextToExist();
      expectHelpTextToNotExist();
    }
  );

  it('does not show help text if module is bypassed', async () => {
    setup(
      {
        ...createProps(),
        profile: createProfileWithComplianceTraining(
          null,
          null,
          nowPlusDays(-1)
        ),
      },
      {
        ...defaultServerConfig,
        complianceTrainingRenewalLookback: 30,
      }
    );

    expectComponentToHaveRendered(AccessTierShortNames.Registered);

    expectHelpTextToNotExist();
    expectMaintenanceTextToNotExist();
  });

  it('does not show help text if module is complete and not expiring', async () => {
    setup(
      {
        ...createProps(),
        tier: AccessTierShortNames.Registered,
        profile: createProfileWithComplianceTraining(
          nowPlusDays(-1),
          nowPlusDays(365),
          null
        ),
      },
      {
        ...defaultServerConfig,
        complianceTrainingRenewalLookback: 30,
      }
    );
    expectComponentToHaveRendered(AccessTierShortNames.Registered);

    expectHelpTextToNotExist();
    expectMaintenanceTextToNotExist();
  });
});
