import '@testing-library/jest-dom';

import * as React from 'react';

import { AccessModule, ConfigResponse, ProfileApi } from 'generated/fetch';

import { render } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { DARTitle, DARTitleProps } from 'app/pages/access/dar-title';
import { queryForCTTitle, queryForRTTitle } from 'app/pages/access/test-utils';
import { createEmptyProfile } from 'app/pages/login/sign-in';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
import { serverConfigStore } from 'app/utils/stores';

import defaultServerConfig from 'testing/default-server-config';
import { ProfileApiStub } from 'testing/stubs/profile-api-stub';

const createProps = (): DARTitleProps => ({
  moduleName: AccessModule.COMPLIANCE_TRAINING,
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
  render(<DARTitle {...props} />);
  return userEvent.setup();
};

describe(DARTitle.name, () => {
  it('renders registered tier training title if module is COMPLIANCE_TRAINING', () => {
    setup({
      ...createProps(),
      moduleName: AccessModule.COMPLIANCE_TRAINING,
    });

    expect(queryForRTTitle()).not.toBeNull();
  });

  it('does not render registered tier training title if module is not COMPLIANCE_TRAINING', () => {
    setup({
      ...createProps(),
      // arbitrary non-COMPLIANCE_TRAINING module
      moduleName: AccessModule.PROFILE_CONFIRMATION,
    });

    expect(queryForRTTitle()).toBeNull();
  });

  it('renders controlled tier training title if module is CT_COMPLIANCE_TRAINING', () => {
    setup({
      ...createProps(),
      moduleName: AccessModule.CT_COMPLIANCE_TRAINING,
    });

    expect(queryForCTTitle()).not.toBeNull();
  });

  it('does not render controlled tier training title if module is not CT_COMPLIANCE_TRAINING', () => {
    setup({
      ...createProps(),
      // arbitrary non-COMPLIANCE_TRAINING module
      moduleName: AccessModule.PROFILE_CONFIRMATION,
    });

    expect(queryForCTTitle()).toBeNull();
  });
});
