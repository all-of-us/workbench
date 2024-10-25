import '@testing-library/jest-dom';

import * as React from 'react';
import { MemoryRouter, Route } from 'react-router';

import {
  AccessModule,
  AccessModuleStatus,
  Authority,
  EgressEventsAdminApi,
  InstitutionalRole,
  InstitutionApi,
  Profile,
  UserAdminApi,
} from 'generated/fetch';

import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AccountCreationOptions } from 'app/pages/login/account-creation/account-creation-options';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
import { AccessTierShortNames } from 'app/utils/access-tiers';
import {
  AccessRenewalStatus,
  getAccessModuleConfig,
} from 'app/utils/access-utils';
import { formatDate, nowPlusDays } from 'app/utils/dates';
import { profileStore, serverConfigStore } from 'app/utils/stores';

import defaultServerConfig from 'testing/default-server-config';
import {
  expectButtonElementDisabled,
  expectButtonElementEnabled,
  getDropdownSelection,
} from 'testing/react-test-helpers';
import { EgressEventsAdminApiStub } from 'testing/stubs/egress-events-admin-api-stub';
import {
  BROAD,
  BROAD_ADDR_1,
  BROAD_ADDR_2,
  InstitutionApiStub,
  VERILY,
  VERILY_WITHOUT_CT,
} from 'testing/stubs/institution-api-stub';
import { ProfileStubVariables } from 'testing/stubs/profile-api-stub';
import { UserAdminApiStub } from 'testing/stubs/user-admin-api-stub';

import { orderedAccessModules } from './admin-user-common';
import { AdminUserProfile } from './admin-user-profile';

const load = jest.fn();
const reload = jest.fn();
const updateCache = jest.fn();

const ADMIN_PROFILE: Profile = {
  ...ProfileStubVariables.PROFILE_STUB,
  authorities: [Authority.ACCESS_CONTROL_ADMIN],
};
const TARGET_USER_PROFILE = ProfileStubVariables.PROFILE_STUB;

const updateTargetProfile = (update: Partial<Profile>) => {
  registerApiClient(
    UserAdminApi,
    new UserAdminApiStub({
      ...TARGET_USER_PROFILE,
      ...update,
    })
  );
};

const getDropdown = (containerTestId: string): HTMLSelectElement => {
  const container = screen.getByTestId(containerTestId);
  return within(container).getByRole('combobox', { hidden: true });
};

describe('AdminUserProfile', () => {
  let user;

  const component = (
    usernameWithoutGsuite: string = ProfileStubVariables.PROFILE_STUB.username
  ) => {
    return render(
      <MemoryRouter initialEntries={[`/admin/users/${usernameWithoutGsuite}`]}>
        <Route path='/admin/users/:usernameWithoutGsuiteDomain'>
          <AdminUserProfile hideSpinner={() => {}} showSpinner={() => {}} />
        </Route>
      </MemoryRouter>
    );
  };

  beforeEach(() => {
    serverConfigStore.set({ config: defaultServerConfig });

    // this represents the admin user who is viewing this profile,
    // NOT the target profile being viewed
    profileStore.set({
      profile: ADMIN_PROFILE,
      load,
      reload,
      updateCache,
    });

    registerApiClient(EgressEventsAdminApi, new EgressEventsAdminApiStub());
    registerApiClient(UserAdminApi, new UserAdminApiStub());
    registerApiClient(InstitutionApi, new InstitutionApiStub());
    user = userEvent.setup();
  });

  const waitUntilPageLoaded = async () => {
    await screen.findByText('User Profile Information');
  };

  it('should render', async () => {
    component();
    await waitUntilPageLoaded();
  });

  it.each([true, false])(
    "should display the user's name, username, initial credits information when enableInitialCreditsExpiration is %s",
    async (enableInitialCreditsExpiration) => {
      const givenName = 'John Q';
      const familyName = 'Public';
      const expectedFullName = 'John Q Public';

      const username = 'some-email@yahoo.com';

      const freeTierUsage = 543.21;
      const freeTierDollarQuota = 678.99;
      const creditUsageText = '$543.21';
      const creditLimitText = '$678.99';
      const initialCreditsExpirationEpochMillis = Date.now();

      serverConfigStore.set({
        config: {
          ...defaultServerConfig,
          enableInitialCreditsExpiration,
        },
      });

      updateTargetProfile({
        username,
        givenName,
        familyName,
        freeTierUsage,
        freeTierDollarQuota,
        initialCreditsExpirationEpochMillis,
      });

      const { container } = component();
      await waitUntilPageLoaded();
      expect(
        within(screen.getByTestId('name')).getByText(expectedFullName)
      ).toBeInTheDocument();
      expect(
        within(screen.getByTestId('user-name')).getByText(username)
      ).toBeInTheDocument();

      expect(screen.getByText(creditUsageText)).toBeInTheDocument();
      expect(
        getDropdownSelection(container, 'initial-credits-dropdown')
      ).toEqual(creditLimitText);

      const expirationMessage = `Credits expired on ${formatDate(
        initialCreditsExpirationEpochMillis,
        '-'
      )}`;

      if (enableInitialCreditsExpiration) {
        expect(screen.getByText(expirationMessage)).toBeInTheDocument();
      } else {
        expect(screen.queryByText(expirationMessage)).not.toBeInTheDocument();
      }
    }
  );

  it.each([true, false])(
    'should display when a user is bypassed when enableInitialCreditsExpiration is %s',
    async (enableInitialCreditsExpiration) => {
      serverConfigStore.set({
        config: {
          ...defaultServerConfig,
          enableInitialCreditsExpiration,
        },
      });
      const initialCreditsExpirationBypassed = true;

      updateTargetProfile({
        initialCreditsExpirationBypassed,
      });

      component();
      await waitUntilPageLoaded();

      if (enableInitialCreditsExpiration) {
        expect(screen.getByText('Credits will not expire')).toBeInTheDocument();
      } else {
        expect(
          screen.queryByText('Credits will not expire')
        ).not.toBeInTheDocument();
      }
    }
  );

  test.each([
    [
      'RT only',
      [AccessTierShortNames.Registered],
      ['registered-tier-badge.svg'],
    ],
    [
      'CT only',
      [AccessTierShortNames.Controlled],
      ['controlled-tier-badge.svg'],
    ],
    [
      'RT and CT',
      [AccessTierShortNames.Registered, AccessTierShortNames.Controlled],
      ['registered-tier-badge.svg', 'controlled-tier-badge.svg'],
    ],
    ['neither', [], ['No data access']],
  ])(
    'should display access tiers if the user has membership in %s',
    async (_, accessTierShortNames, expectedText) => {
      updateTargetProfile({ accessTierShortNames });

      component();
      await waitUntilPageLoaded();
      screen.logTestingPlaygroundURL();
      expectedText.map((text) => {
        expect(
          within(screen.getByTestId('data-access-tiers')).getByText(text)
        ).toBeInTheDocument();
      });
    }
  );

  it('should allow updating contactEmail within an institution', async () => {
    updateTargetProfile({ contactEmail: BROAD_ADDR_1 });

    component();
    await waitUntilPageLoaded();

    const contactEmailInput: HTMLInputElement = screen.getByRole('textbox', {
      name: /contact email/i,
    });

    expect(contactEmailInput).toHaveValue(BROAD_ADDR_1);

    await user.clear(contactEmailInput);
    await user.click(contactEmailInput);
    await user.paste(BROAD_ADDR_2);
    await user.tab();

    expect(contactEmailInput).toHaveValue(BROAD_ADDR_2);

    expect(screen.queryByTestId('email-invalid')).not.toBeInTheDocument();

    const saveButton = screen.getByRole('button', {
      name: /save/i,
    });
    expectButtonElementEnabled(saveButton);
  });

  it("should prohibit updating contactEmail if it doesn't match institution ADDRESSES", async () => {
    updateTargetProfile({ contactEmail: BROAD_ADDR_1 });

    component();
    await waitUntilPageLoaded();

    const contactEmailInput: HTMLInputElement = screen.getByRole('textbox', {
      name: /contact email/i,
    });

    expect(contactEmailInput).toHaveValue(BROAD_ADDR_1);

    const nonBroadAddr = 'PI@rival-institute.net';

    await user.clear(contactEmailInput);
    await user.click(contactEmailInput);
    await user.paste(nonBroadAddr);
    await user.tab();

    expect(contactEmailInput).toHaveValue(nonBroadAddr);

    const invalidEmail = await screen.findByTestId('email-invalid');
    within(invalidEmail).getByText(
      /The institution has authorized access only to select members./i
    );

    const saveButton = screen.getByRole('button', {
      name: /save/i,
    });
    expectButtonElementDisabled(saveButton);
  });

  it("should prohibit updating contactEmail if it doesn't match institution DOMAINS", async () => {
    const originalAddress = 'researcher@verily.com';
    updateTargetProfile({
      verifiedInstitutionalAffiliation: {
        ...TARGET_USER_PROFILE.verifiedInstitutionalAffiliation,
        institutionShortName: VERILY.shortName,
      },
      contactEmail: originalAddress,
    });

    component();
    await waitUntilPageLoaded();

    const contactEmailInput: HTMLInputElement = screen.getByRole('textbox', {
      name: /contact email/i,
    });

    expect(contactEmailInput).toHaveValue(originalAddress);

    const nonVerilyAddr = 'PI@rival-institute.net';
    await user.clear(contactEmailInput);
    await user.click(contactEmailInput);
    await user.paste(nonVerilyAddr);
    await user.tab();
    expect(contactEmailInput).toHaveValue(nonVerilyAddr);

    const invalidEmail = await screen.findByTestId('email-invalid');
    within(invalidEmail).getByText(
      /Your email does not match your institution/i
    );

    expectButtonElementDisabled(
      screen.getByRole('button', {
        name: /save/i,
      })
    );
  });

  it('should allow updating institution if the email continues to match', async () => {
    const contactEmail = 'user1@google.com';
    updateTargetProfile({
      verifiedInstitutionalAffiliation: {
        ...TARGET_USER_PROFILE.verifiedInstitutionalAffiliation,
        institutionShortName: VERILY.shortName,
      },
      contactEmail,
    });

    component();
    await waitUntilPageLoaded();

    const verifiedInstitutionDropdown = getDropdown('verifiedInstitution');

    expect(verifiedInstitutionDropdown.value).toEqual(VERILY.shortName);

    await user.click(verifiedInstitutionDropdown);

    await user.click(screen.getByText(VERILY_WITHOUT_CT.shortName));

    expect(verifiedInstitutionDropdown.value).toEqual(
      VERILY_WITHOUT_CT.shortName
    );
    expect(screen.queryByTestId('email-invalid')).not.toBeInTheDocument();

    // can't save yet - still need to set the role

    const saveButton = screen.getByRole('button', {
      name: /save/i,
    });
    expectButtonElementDisabled(saveButton);
    const institutionalRoleDropdown = getDropdown('institutionalRole');
    await user.click(institutionalRoleDropdown);

    const postDocLabel = AccountCreationOptions.institutionalRoleOptions.filter(
      (option) => option.value === InstitutionalRole.POST_DOCTORAL
    )[0].label;
    await user.click(screen.getByText(postDocLabel));
    expect(institutionalRoleDropdown.value).toEqual(
      InstitutionalRole.POST_DOCTORAL
    );
    expectButtonElementEnabled(saveButton);
  });

  it('should not allow updating institution if the email no longer matches', async () => {
    const contactEmail = 'user1@google.com';
    updateTargetProfile({
      verifiedInstitutionalAffiliation: {
        ...TARGET_USER_PROFILE.verifiedInstitutionalAffiliation,
        institutionShortName: VERILY.shortName,
      },
      contactEmail,
    });

    component();
    await waitUntilPageLoaded();

    const verifiedInstitutionDropdown = getDropdown('verifiedInstitution');

    expect(verifiedInstitutionDropdown.value).toEqual(VERILY.shortName);

    await user.click(verifiedInstitutionDropdown);
    await user.click(screen.getByText(BROAD.displayName));

    expect(verifiedInstitutionDropdown.value).toEqual(BROAD.shortName);

    screen.getByTestId('email-invalid');

    // also need to set the Institutional Role
    const institutionalRoleDropdown = getDropdown('institutionalRole');

    await user.click(institutionalRoleDropdown);

    const postDocLabel = AccountCreationOptions.institutionalRoleOptions.filter(
      (option) => option.value === InstitutionalRole.POST_DOCTORAL
    )[0].label;
    await user.click(screen.getByText(postDocLabel));
    expect(institutionalRoleDropdown.value).toEqual(
      InstitutionalRole.POST_DOCTORAL
    );

    expectButtonElementDisabled(
      screen.getByRole('button', {
        name: /save/i,
      })
    );
  });

  it('should allow updating both email and institution if they match each other', async () => {
    const contactEmail = 'user1@google.com';
    updateTargetProfile({
      verifiedInstitutionalAffiliation: {
        ...TARGET_USER_PROFILE.verifiedInstitutionalAffiliation,
        institutionShortName: VERILY.shortName,
      },
      contactEmail,
    });

    component();
    await waitUntilPageLoaded();

    const verifiedInstitutionDropdown = getDropdown('verifiedInstitution');
    expect(verifiedInstitutionDropdown.value).toEqual(VERILY.shortName);

    await user.click(verifiedInstitutionDropdown);
    await user.click(screen.getByText(BROAD.displayName));

    expect(verifiedInstitutionDropdown.value).toEqual(BROAD.shortName);

    // also need to set the Institutional Role
    const institutionalRoleDropdown = getDropdown('institutionalRole');

    await user.click(institutionalRoleDropdown);
    const postDocLabel = AccountCreationOptions.institutionalRoleOptions.filter(
      (option) => option.value === InstitutionalRole.POST_DOCTORAL
    )[0].label;
    await user.click(screen.getByText(postDocLabel));
    expect(institutionalRoleDropdown.value).toEqual(
      InstitutionalRole.POST_DOCTORAL
    );

    const contactEmailInput: HTMLInputElement = screen.getByRole('textbox', {
      name: /contact email/i,
    });

    await user.clear(contactEmailInput);
    await user.click(contactEmailInput);
    await user.paste(BROAD_ADDR_1);
    await user.tab();

    expect(contactEmailInput).toHaveValue(BROAD_ADDR_1);

    expect(screen.queryByTestId('email-invalid')).not.toBeInTheDocument();

    expectButtonElementEnabled(
      screen.getByRole('button', {
        name: /save/i,
      })
    );
  });

  it('should prohibit updating institutional role to Other without adding other-text', async () => {
    const contactEmail = 'user1@google.com';
    updateTargetProfile({
      verifiedInstitutionalAffiliation: {
        ...TARGET_USER_PROFILE.verifiedInstitutionalAffiliation,
        institutionShortName: VERILY.shortName,
      },
      contactEmail,
    });

    component();
    await waitUntilPageLoaded();

    const institutionalRoleDropdown = getDropdown('institutionalRole');

    await user.click(institutionalRoleDropdown);
    const otherLabel = AccountCreationOptions.institutionalRoleOptions.filter(
      (option) => option.value === InstitutionalRole.OTHER
    )[0].label;
    await user.click(screen.getByText(otherLabel));
    expect(institutionalRoleDropdown.value).toEqual(InstitutionalRole.OTHER);

    expectButtonElementDisabled(
      screen.getByRole('button', {
        name: /save/i,
      })
    );

    // now update other-text
    const institutionalRoleDescriptionInput: HTMLInputElement =
      screen.getByRole('textbox', {
        name: /institutional role description/i,
      });

    const roleDetails = 'I do a science';
    await user.clear(institutionalRoleDescriptionInput);
    await user.click(institutionalRoleDescriptionInput);
    await user.paste(roleDetails);
    await user.tab();

    expect(institutionalRoleDescriptionInput).toHaveValue(roleDetails);

    expectButtonElementEnabled(
      screen.getByRole('button', {
        name: /save/i,
      })
    );
  });

  it('should allow updating initial credit limit', async () => {
    component();
    await waitUntilPageLoaded();

    const initialCreditsDropdown = getDropdown('initial-credits-dropdown');

    expect(initialCreditsDropdown.value).toEqual(
      TARGET_USER_PROFILE.freeTierDollarQuota.toString()
    );

    const newLimit = 800.0;
    expect(newLimit).not.toEqual(TARGET_USER_PROFILE.freeTierDollarQuota); // sanity check

    await user.click(initialCreditsDropdown);
    await user.click(screen.getByText(`\$${newLimit.toFixed(2)}`));

    expect(initialCreditsDropdown.value).toEqual(newLimit.toString());

    expectButtonElementEnabled(
      screen.getByRole('button', {
        name: /save/i,
      })
    );
  });

  function expectModuleTitlesInOrder(
    accessModules: AccessModule[],
    tableRows: HTMLElement[]
  ) {
    accessModules.forEach((moduleName, index) => {
      const moduleRow = tableRows[index];
      const { adminPageTitle } = getAccessModuleConfig(moduleName);
      expect(within(moduleRow).getByText(adminPageTitle)).toBeInTheDocument();
    });
  }

  it('should render the titles of all expected access modules', async () => {
    component();
    await waitUntilPageLoaded();

    const table = screen.getByTestId('access-module-table');
    const rows = within(table).getAllByRole('row');
    rows.shift(); // remove the header row
    expect(rows).toHaveLength(orderedAccessModules.length);

    // confirm that the orderedAccessModules are listed in order with expected title text
    expectModuleTitlesInOrder(orderedAccessModules, rows);
  });

  test.each([
    [
      AccessRenewalStatus.EXPIRED,
      AccessModule.COMPLIANCE_TRAINING,
      {
        moduleName: AccessModule.COMPLIANCE_TRAINING,
        completionEpochMillis: nowPlusDays(-1000),
        expirationEpochMillis: nowPlusDays(-1),
      },
    ],
    [
      AccessRenewalStatus.NEVER_EXPIRES,
      AccessModule.TWO_FACTOR_AUTH,
      {
        moduleName: AccessModule.TWO_FACTOR_AUTH,
        completionEpochMillis: nowPlusDays(-1000),
      },
    ],
    [
      AccessRenewalStatus.INCOMPLETE,
      AccessModule.IDENTITY,
      {
        moduleName: AccessModule.IDENTITY,
      },
    ],
    [
      AccessRenewalStatus.CURRENT,
      AccessModule.CT_COMPLIANCE_TRAINING,
      {
        moduleName: AccessModule.CT_COMPLIANCE_TRAINING,
        completionEpochMillis: nowPlusDays(-1000),
        expirationEpochMillis: nowPlusDays(400),
      },
    ],
    [
      AccessRenewalStatus.EXPIRING_SOON,
      AccessModule.PROFILE_CONFIRMATION,
      {
        moduleName: AccessModule.PROFILE_CONFIRMATION,
        completionEpochMillis: nowPlusDays(-1000),
        expirationEpochMillis: nowPlusDays(5),
      },
    ],
  ])(
    "should render a(n) '%s' completion status for access module %s",
    async (
      expectedStatus: AccessRenewalStatus,
      moduleName: AccessModule,
      moduleStatus: AccessModuleStatus
    ) => {
      const statusesExceptThisOne =
        TARGET_USER_PROFILE.accessModules.modules.filter(
          (s) => s.moduleName !== moduleName
        );
      updateTargetProfile({
        accessModules: {
          ...TARGET_USER_PROFILE.accessModules,
          modules: [...statusesExceptThisOne, moduleStatus],
        },
      });
      component();
      await waitUntilPageLoaded();

      const table = screen.getByTestId('access-module-table');
      const rows = within(table).getAllByRole('row');
      rows.shift(); // remove the header row
      expect(rows).toHaveLength(orderedAccessModules.length);

      // the previous test confirmed that the orderedAccessModules are in the expected order, so we can ref by index

      const { adminPageTitle } = getAccessModuleConfig(moduleName);
      const moduleRow = rows[orderedAccessModules.indexOf(moduleName)];
      // sanity check - this is actually the right row for this module
      expect(within(moduleRow).getByText(adminPageTitle)).toBeInTheDocument();
      expect(within(moduleRow).getByText(expectedStatus)).toBeInTheDocument();
    }
  );

  it('should skip access modules when they are disabled in the environment', async () => {
    serverConfigStore.set({
      config: {
        ...defaultServerConfig,
        enableRasLoginGovLinking: false,
        enableComplianceTraining: false,
      },
    });

    const excludedModules: Array<AccessModule> = [
      AccessModule.IDENTITY,
      AccessModule.ERA_COMMONS,
      AccessModule.COMPLIANCE_TRAINING,
      AccessModule.CT_COMPLIANCE_TRAINING,
    ];
    const expectedModules = orderedAccessModules.filter(
      (moduleName) => !excludedModules.includes(moduleName)
    );

    component();
    await waitUntilPageLoaded();

    const table = screen.getByTestId('access-module-table');
    const rows = within(table).getAllByRole('row');
    rows.shift(); // remove the header row
    expect(rows).toHaveLength(expectedModules.length);

    // confirm that the expectedModules are listed in order with expected title text
    expectModuleTitlesInOrder(expectedModules, rows);
  });
});
