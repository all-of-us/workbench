import * as React from 'react';
import { MemoryRouter, Route } from 'react-router';
import { mount, ReactWrapper } from 'enzyme';

import {
  Authority,
  EgressEventsAdminApi,
  Profile,
  UserAdminApi,
} from 'generated/fetch';

import { registerApiClient } from 'app/services/swagger-fetch-clients';
import {
  AccessTierDisplayNames,
  AccessTierShortNames,
} from 'app/utils/access-tiers';
import { profileStore, serverConfigStore } from 'app/utils/stores';

import defaultServerConfig from 'testing/default-server-config';
import { waitOneTickAndUpdate } from 'testing/react-test-helpers';
import { EgressEventsAdminApiStub } from 'testing/stubs/egress-events-admin-api-stub';
import { ProfileStubVariables } from 'testing/stubs/profile-api-stub';
import { UserAdminApiStub } from 'testing/stubs/user-admin-api-stub';

import { AdminUserProfile } from './admin-user-profile';

const load = jest.fn();
const reload = jest.fn();
const updateCache = jest.fn();

const ADMIN_PROFILE: Profile = {
  ...ProfileStubVariables.PROFILE_STUB,
  authorities: [Authority.ACCESSCONTROLADMIN],
};
const USER_PROFILE = ProfileStubVariables.PROFILE_STUB;

const updateTargetProfile = (update: Partial<Profile>) => {
  registerApiClient(
    UserAdminApi,
    new UserAdminApiStub({
      ...USER_PROFILE,
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

describe('AdminUserProfile', () => {
  const component = (
    usernameWithoutGsuite: string = ProfileStubVariables.PROFILE_STUB.username
  ) => {
    return mount(
      <MemoryRouter
        initialEntries={[`/admin/users-tmp/${usernameWithoutGsuite}`]}
      >
        <Route path='/admin/users-tmp/:usernameWithoutGsuiteDomain'>
          <AdminUserProfile hideSpinner={() => {}} showSpinner={() => {}} />
        </Route>
      </MemoryRouter>
    );
  };

  beforeEach(() => {
    serverConfigStore.set({ config: defaultServerConfig });
    profileStore.set({
      profile: ADMIN_PROFILE,
      load,
      reload,
      updateCache,
    });

    registerApiClient(EgressEventsAdminApi, new EgressEventsAdminApiStub());
    registerApiClient(UserAdminApi, new UserAdminApiStub());
  });

  it('should render', () => {
    const wrapper = component();
    expect(wrapper).toBeTruthy();
  });

  it("should display the user's name, username, and initial credits usage", async () => {
    const givenName = 'John Q';
    const familyName = 'Public';
    const expectedFullName = 'John Q Public';

    const username = 'a-new-email@yahoo.com';

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

    const wrapper = component();
    expect(wrapper).toBeTruthy();
    await waitOneTickAndUpdate(wrapper);

    expect(getUneditableFieldText(wrapper, 'name')).toEqual(expectedFullName);
    expect(getUneditableFieldText(wrapper, 'user-name')).toEqual(username);
    expect(getUneditableFieldText(wrapper, 'initial-credits-used')).toEqual(
      expectedCreditsText
    );
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

      const wrapper = component();
      expect(wrapper).toBeTruthy();
      await waitOneTickAndUpdate(wrapper);

      expect(getUneditableFieldText(wrapper, 'data-access-tiers')).toEqual(
        expectedText
      );
    }
  );
});
