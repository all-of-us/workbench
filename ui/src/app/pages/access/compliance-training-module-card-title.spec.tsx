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
import { createEmptyProfile } from 'app/pages/login/sign-in';
import {
  profileApi,
  registerApiClient,
} from 'app/services/swagger-fetch-clients';
import { AccessTierShortNames } from 'app/utils/access-tiers';
import { nowPlusDays } from 'app/utils/dates';
import { serverConfigStore } from 'app/utils/stores';

import defaultServerConfig from 'testing/default-server-config';
import { ProfileApiStub } from 'testing/stubs/profile-api-stub';

const findHelpText = () => {
  return screen.findByText(
    /navigate to "my courses" and select "responsible conduct of research"/i
  );
};

const expectHelpTextToExist = async () => {
  expect(await findHelpText()).not.toBeNull();
};

const expectHelpTextToNotExist = async () => {
  try {
    await findHelpText();
    // The above throws an error if the element is not found
    expect(true).toBeFalsy();
  } catch (e) {
    // Expected behavior, do nothing
  }
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
  config: ConfigResponse = { ...defaultServerConfig },
  useAbsorb: boolean = false
) => {
  registerApiClient(ProfileApi, new ProfileApiStub());
  profileApi().useAbsorb = () => Promise.resolve(useAbsorb);
  serverConfigStore.set({ config });
  return {
    container: render(<ComplianceTrainingModuleCardTitle {...props} />)
      .container,
    user: userEvent.setup(),
  };
};

export const queryForRTTitle = () => {
  return screen.queryByText(/responsible conduct of research training/i);
};

export const queryForCTTitle = () => {
  return screen.queryByText(/controlled tier training/i);
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
      defaultServerConfig,
      true
    );

    await expectHelpTextToExist();
  });

  it('shows help text if absorb is used and module is expiring', async () => {
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
      },
      true
    );

    await expectHelpTextToExist();
  });

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

    await expectHelpTextToNotExist();
  });

  it('does not show help text if absorb is not used', async () => {
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
      },
      false
    );

    await expectHelpTextToNotExist();
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

    await expectHelpTextToNotExist();
  });
});
