import * as React from 'react';
import { MemoryRouter, Route } from 'react-router';
import { mount, ReactWrapper } from 'enzyme';
import { Dropdown } from 'primereact/dropdown';

import {
  AccessModule,
  AccessModuleStatus,
  Authority,
  EgressEventsAdminApi,
  InstitutionalRole,
  InstitutionApi,
  Profile,
  UserAdminApi,
  UserTierEligibility,
} from 'generated/fetch';

import { AccountCreationOptions } from '../../login/account-creation/account-creation-options';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
import {
  AccessTierDisplayNames,
  AccessTierShortNames,
} from 'app/utils/access-tiers';
import {
  AccessRenewalStatus,
  getAccessModuleConfig,
} from 'app/utils/access-utils';
import { nowPlusDays } from 'app/utils/dates';
import { profileStore, serverConfigStore } from 'app/utils/stores';

import defaultServerConfig from 'testing/default-server-config';
import {
  expectButtonElementDisabled,
  expectButtonElementEnabled,
  findNodesContainingText,
  simulateComponentChange,
  simulateTextInputChange,
  waitForNoSpinner,
  waitOneTickAndUpdate,
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

const getUneditableFieldText = (
  wrapper: ReactWrapper,
  dataTestId: string
): string => {
  const divs = wrapper.find(`[data-test-id="${dataTestId}"]`).find('div');

  // sanity check: divs should contain [parent, label, value]
  expect(divs.length).toEqual(3);
  return divs.at(2).text();
};

const findDropdown = (wrapper: ReactWrapper, dataTestId: string) =>
  wrapper.find(`[data-test-id="${dataTestId}"]`).find(Dropdown).first();

const findTextInput = (wrapper: ReactWrapper, dataTestId: string) =>
  wrapper.find(`[data-test-id="${dataTestId}"]`).first();

const getDropdown = (containerTestId: string): HTMLSelectElement => {
  const container = screen.getByTestId(containerTestId);
  return within(container).getByRole('combobox', { hidden: true });
};

describe('AdminUserProfile', () => {
  let user;
  const component = (
    usernameWithoutGsuite: string = ProfileStubVariables.PROFILE_STUB.username
  ) => {
    return mount(
      <MemoryRouter initialEntries={[`/admin/users/${usernameWithoutGsuite}`]}>
        <Route path='/admin/users/:usernameWithoutGsuiteDomain'>
          <AdminUserProfile hideSpinner={() => {}} showSpinner={() => {}} />
        </Route>
      </MemoryRouter>
    );
  };

  const componentAlt = (
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

  it('should render', () => {
    componentAlt();
    screen.findByText('User Profile Information');
  });

  it("should display the user's name, username, and initial credits usage", async () => {
    const givenName = 'John Q';
    const familyName = 'Public';
    const expectedFullName = 'John Q Public';

    const username = 'some-email@yahoo.com';

    const freeTierUsage = 543.21;
    const freeTierDollarQuota = 678.99;
    const expectedCreditsText = '$543.21 used of $678.99 limit';

    updateTargetProfile({
      username,
      givenName,
      familyName,
      freeTierUsage,
      freeTierDollarQuota,
    });

    componentAlt();
    await waitForNoSpinner();
    expect(
      within(screen.getByTestId('name')).getByText(expectedFullName)
    ).toBeInTheDocument();
    expect(
      within(screen.getByTestId('user-name')).getByText(username)
    ).toBeInTheDocument();
    expect(
      within(screen.getByTestId('initial-credits-used')).getByText(
        expectedCreditsText
      )
    ).toBeInTheDocument();
  });

  test.each([
    [
      'RT only',
      [AccessTierShortNames.Registered],
      AccessTierDisplayNames.Registered,
    ],
    [
      'CT only',
      [AccessTierShortNames.Controlled],
      AccessTierDisplayNames.Controlled,
    ],
    [
      'RT and CT',
      [AccessTierShortNames.Registered, AccessTierShortNames.Controlled],
      'Registered Tier, Controlled Tier',
    ],
    ['neither', [], 'No data access'],
  ])(
    'should display access tiers if the user has membership in %s',
    async (_, accessTierShortNames, expectedText) => {
      updateTargetProfile({ accessTierShortNames });

      componentAlt();
      await waitForNoSpinner();
      expect(
        within(screen.getByTestId('data-access-tiers')).getByText(expectedText)
      ).toBeInTheDocument();
    }
  );

  it('should allow updating contactEmail within an institution', async () => {
    updateTargetProfile({ contactEmail: BROAD_ADDR_1 });

    componentAlt();
    await waitForNoSpinner();

    const contactEmailInput: HTMLInputElement = screen.getByRole('textbox', {
      name: /contact email/i,
    });

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

    componentAlt();
    await waitForNoSpinner();

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

    componentAlt();
    await waitForNoSpinner();

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

    componentAlt();
    await waitForNoSpinner();

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

    componentAlt();
    await waitForNoSpinner();

    const verifiedInstitutionDropdown = getDropdown('verifiedInstitution');

    expect(verifiedInstitutionDropdown.value).toEqual(VERILY.shortName);

    await user.click(verifiedInstitutionDropdown);
    screen.logTestingPlaygroundURL();
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

  it('should not allow updating both email and institution if they match each other', async () => {
    const contactEmail = 'user1@google.com';
    updateTargetProfile({
      verifiedInstitutionalAffiliation: {
        ...TARGET_USER_PROFILE.verifiedInstitutionalAffiliation,
        institutionShortName: VERILY.shortName,
      },
      contactEmail,
    });

    const wrapper = component();
    expect(wrapper).toBeTruthy();
    await waitOneTickAndUpdate(wrapper);

    expect(findDropdown(wrapper, 'verifiedInstitution').props().value).toEqual(
      VERILY.shortName
    );

    await simulateComponentChange(
      wrapper,
      findDropdown(wrapper, 'verifiedInstitution'),
      BROAD.shortName
    );
    expect(findDropdown(wrapper, 'verifiedInstitution').props().value).toEqual(
      BROAD.shortName
    );

    // also need to set the Institutional Role

    await simulateComponentChange(
      wrapper,
      findDropdown(wrapper, 'institutionalRole'),
      InstitutionalRole.POST_DOCTORAL
    );
    expect(findDropdown(wrapper, 'institutionalRole').props().value).toEqual(
      InstitutionalRole.POST_DOCTORAL
    );

    await simulateTextInputChange(
      findTextInput(wrapper, 'contactEmail'),
      BROAD_ADDR_1
    );
    expect(findTextInput(wrapper, 'contactEmail').props().value).toEqual(
      BROAD_ADDR_1
    );

    expect(wrapper.find('[data-test-id="email-invalid"]').exists()).toBeFalsy();

    const saveButton = wrapper.find('[data-test-id="update-profile"]');
    expect(saveButton.exists()).toBeTruthy();
    expect(saveButton.props().disabled).toBeFalsy();
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

    const wrapper = component();
    expect(wrapper).toBeTruthy();
    await waitOneTickAndUpdate(wrapper);

    await simulateComponentChange(
      wrapper,
      findDropdown(wrapper, 'institutionalRole'),
      InstitutionalRole.OTHER
    );
    expect(findDropdown(wrapper, 'institutionalRole').props().value).toEqual(
      InstitutionalRole.OTHER
    );

    let saveButton = wrapper.find('[data-test-id="update-profile"]');
    expect(saveButton.exists()).toBeTruthy();
    expect(saveButton.props().disabled).toBeTruthy();

    // now update other-text

    expect(
      findTextInput(wrapper, 'institutionalRoleOtherText').exists()
    ).toBeTruthy();
    const roleDetails = 'I do a science';
    await simulateTextInputChange(
      findTextInput(wrapper, 'institutionalRoleOtherText'),
      roleDetails
    );
    expect(
      findTextInput(wrapper, 'institutionalRoleOtherText').props().value
    ).toEqual(roleDetails);

    saveButton = wrapper.find('[data-test-id="update-profile"]');
    expect(saveButton.exists()).toBeTruthy();
    expect(saveButton.props().disabled).toBeFalsy();
  });

  it('should allow updating initial credit limit', async () => {
    const wrapper = component();
    expect(wrapper).toBeTruthy();
    await waitOneTickAndUpdate(wrapper);

    expect(
      findDropdown(wrapper, 'initial-credits-dropdown').props().value
    ).toEqual(TARGET_USER_PROFILE.freeTierDollarQuota);

    const newLimit = 800.0;
    expect(newLimit).not.toEqual(TARGET_USER_PROFILE.freeTierDollarQuota); // sanity check

    await simulateComponentChange(
      wrapper,
      findDropdown(wrapper, 'initial-credits-dropdown'),
      newLimit
    );
    expect(
      findDropdown(wrapper, 'initial-credits-dropdown').props().value
    ).toEqual(newLimit);

    const saveButton = wrapper.find('[data-test-id="update-profile"]');
    expect(saveButton.exists()).toBeTruthy();
    expect(saveButton.props().disabled).toBeFalsy();
  });

  function expectModuleTitlesInOrder(
    accessModules: AccessModule[],
    tableRows: ReactWrapper
  ) {
    accessModules.forEach((moduleName, index) => {
      const moduleRow = tableRows.at(index);
      const { adminPageTitle } = getAccessModuleConfig(moduleName);
      expect(
        findNodesContainingText(moduleRow, adminPageTitle).exists()
      ).toBeTruthy();
    });
  }

  it('should render the titles of all expected access modules', async () => {
    const wrapper = component();
    expect(wrapper).toBeTruthy();
    await waitOneTickAndUpdate(wrapper);

    const table = wrapper.find('[data-test-id="access-module-table"]');
    expect(table.exists()).toBeTruthy();

    const tableRows = table.find('tbody tr[role="row"]');
    expect(tableRows.length).toEqual(orderedAccessModules.length);

    // confirm that the orderedAccessModules are listed in order with expected title text
    expectModuleTitlesInOrder(orderedAccessModules, tableRows);
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
      AccessRenewalStatus.BYPASSED,
      AccessModule.ERA_COMMONS,
      {
        moduleName: AccessModule.ERA_COMMONS,
        bypassEpochMillis: 1,
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
      const wrapper = component();
      expect(wrapper).toBeTruthy();
      await waitOneTickAndUpdate(wrapper);

      const tableRows = wrapper
        .find('[data-test-id="access-module-table"]')
        .find('tbody tr[role="row"]');
      expect(tableRows.length).toEqual(orderedAccessModules.length);

      // the previous test confirmed that the orderedAccessModules are in the expected order, so we can ref by index

      const { adminPageTitle } = getAccessModuleConfig(moduleName);
      const moduleRow = tableRows.at(orderedAccessModules.indexOf(moduleName));
      // sanity check - this is actually the right row for this module
      expect(
        findNodesContainingText(moduleRow, adminPageTitle).exists()
      ).toBeTruthy();

      expect(
        findNodesContainingText(moduleRow, expectedStatus).exists()
      ).toBeTruthy();
    }
  );

  it('should skip access modules when they are disabled in the environment', async () => {
    serverConfigStore.set({
      config: {
        ...defaultServerConfig,
        enableRasLoginGovLinking: false,
        enableEraCommons: false,
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

    const wrapper = component();
    expect(wrapper).toBeTruthy();
    await waitOneTickAndUpdate(wrapper);

    const tableRows = wrapper
      .find('[data-test-id="access-module-table"]')
      .find('tbody tr[role="row"]');
    expect(tableRows.length).toEqual(expectedModules.length);

    // confirm that the expectedModules are listed in order with expected title text
    expectModuleTitlesInOrder(expectedModules, tableRows);
  });

  test.each([
    [
      'Registered Tier',
      [
        {
          accessTierShortName: AccessTierShortNames.Registered,
          eraRequired: true,
        },
        {
          accessTierShortName: AccessTierShortNames.Controlled,
          eraRequired: false,
        },
      ],
      true,
      false,
    ],
    [
      'Controlled Tier',
      [
        {
          accessTierShortName: AccessTierShortNames.Registered,
          eraRequired: false,
        },
        {
          accessTierShortName: AccessTierShortNames.Controlled,
          eraRequired: true,
        },
      ],
      false,
      true,
    ],
    [
      'Registered Tier and Controlled Tier',
      [
        {
          accessTierShortName: AccessTierShortNames.Registered,
          eraRequired: true,
        },
        {
          accessTierShortName: AccessTierShortNames.Controlled,
          eraRequired: true,
        },
      ],
      true,
      true,
    ],
  ])(
    'should indicate when eRA Commons is required for %s',
    async (
      tiers: string,
      tierEligibilities: UserTierEligibility[],
      rtBadgeExpected: boolean,
      ctBadgeExpected: boolean
    ) => {
      updateTargetProfile({
        tierEligibilities,
      });

      const wrapper = component();
      expect(wrapper).toBeTruthy();
      await waitOneTickAndUpdate(wrapper);

      const moduleTable = wrapper.find('[data-test-id="access-module-table"]');
      expect(
        findNodesContainingText(
          moduleTable,
          `requires eRA Commons for ${tiers} access`
        ).exists()
      ).toBeTruthy();

      const tableRows = moduleTable.find('tbody tr[role="row"]');
      expect(tableRows.length).toEqual(orderedAccessModules.length);

      // a previous test confirmed that the orderedAccessModules are in the expected order, so we can ref by index

      const eraRow = tableRows.at(
        orderedAccessModules.indexOf(AccessModule.ERA_COMMONS)
      );
      const eraBadges = eraRow.find('[data-test-id="tier-badges"]');

      expect(
        findNodesContainingText(eraBadges, 'registered-tier-badge.svg').exists()
      ).toBe(rtBadgeExpected);
      expect(
        findNodesContainingText(eraBadges, 'controlled-tier-badge.svg').exists()
      ).toBe(ctBadgeExpected);
    }
  );
});
