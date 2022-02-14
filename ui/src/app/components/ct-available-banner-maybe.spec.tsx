import * as React from 'react';
import { MemoryRouter } from 'react-router-dom';
import { mount } from 'enzyme';

import { CdrVersionTier, Profile, UserTierEligibility } from 'generated/fetch';

import { environment } from 'environments/environment';
import { AccessTierShortNames } from 'app/utils/access-tiers';
import { DATA_ACCESS_REQUIREMENTS_PATH } from 'app/utils/access-utils';
import { cdrVersionStore, profileStore } from 'app/utils/stores';

import { waitOneTickAndUpdate } from 'testing/react-test-helpers';
import { cdrVersionTiersResponse } from 'testing/stubs/cdr-versions-api-stub';
import { ProfileStubVariables } from 'testing/stubs/profile-api-stub';

import { CTAvailableBannerMaybe } from './ct-available-banner-maybe';

// 3 times, in order
const [TIME1, TIME2, TIME3] = [10000, 20000, 30000];

describe('CTAvailableBannerMaybe', () => {
  const load = jest.fn();
  const reload = jest.fn();
  const updateCache = jest.fn();

  const component = () => {
    return mount(
      <MemoryRouter>
        <CTAvailableBannerMaybe />
      </MemoryRouter>
    );
  };

  const ctBannerExists = (wrapper) =>
    wrapper.find('[data-test-id="controlled-tier-available"]').exists();

  interface ProfileUpdate {
    newTierEligibilities?: UserTierEligibility[];
    newUserTiers?: string[];
    newFirstSignIn?: number;
  }
  const updateProfile = (props: ProfileUpdate) => {
    const originalProfile = ProfileStubVariables.PROFILE_STUB;

    const profile: Profile = {
      ...originalProfile,
      tierEligibilities:
        props.newTierEligibilities ?? originalProfile.tierEligibilities,
      accessTierShortNames:
        props.newUserTiers ?? originalProfile.accessTierShortNames,
      firstSignInTime: props.newFirstSignIn ?? originalProfile.firstSignInTime,
    };

    profileStore.set({ profile, load, reload, updateCache });
  };

  const updateCdrVersions = (newTier: CdrVersionTier) => {
    const otherTiers = cdrVersionTiersResponse.tiers.filter(
      (tier) => tier.accessTierShortName !== tier.accessTierShortName
    );
    cdrVersionStore.set({ tiers: [...otherTiers, newTier] });
  };

  const fulfillAllBannerRequirements = () => {
    // the environment allows users to see the CT (in the UI)
    environment.accessTiersVisibleToUsers = [
      AccessTierShortNames.Registered,
      AccessTierShortNames.Controlled,
    ];

    // the user is eligible for the CT
    const newTierEligibilities: UserTierEligibility[] = [
      {
        accessTierShortName: AccessTierShortNames.Registered,
        eraRequired: false,
        eligible: true,
      },
      {
        accessTierShortName: AccessTierShortNames.Controlled,
        eraRequired: false,
        eligible: true,
      },
    ];

    // the user does not currently have CT access
    const newUserTiers: string[] = [AccessTierShortNames.Registered];

    // the user's first sign-in time was before the release of the default CT CDR Version
    const newFirstSignIn = TIME1;
    const controlledTierCdrVersions: CdrVersionTier = {
      ...cdrVersionTiersResponse.tiers.find(
        (t) => t.accessTierShortName === AccessTierShortNames.Controlled
      ),
      defaultCdrVersionCreationTime: TIME2,
    };

    // the user is not currently visiting the DAR page
    window.location.pathname = '/';

    updateProfile({ newTierEligibilities, newUserTiers, newFirstSignIn });
    updateCdrVersions(controlledTierCdrVersions);
  };

  beforeEach(() => {
    profileStore.set({
      profile: ProfileStubVariables.PROFILE_STUB,
      load,
      reload,
      updateCache,
    });
    cdrVersionStore.set(cdrVersionTiersResponse);
  });

  it('should render if all of the requirements are met', () => {
    fulfillAllBannerRequirements();

    const wrapper = component();
    expect(wrapper.exists()).toBeTruthy();

    expect(ctBannerExists(wrapper)).toBeTruthy();
  });

  const ctNotVisible = () => {
    environment.accessTiersVisibleToUsers = [AccessTierShortNames.Registered];
  };
  const userIneligible = () => {
    const newTierEligibilities: UserTierEligibility[] = [
      {
        accessTierShortName: AccessTierShortNames.Registered,
        eraRequired: false,
        eligible: true,
      },
    ];

    updateProfile({ newTierEligibilities });
  };
  const ctAccess = () => {
    updateProfile({
      newUserTiers: [
        AccessTierShortNames.Registered,
        AccessTierShortNames.Controlled,
      ],
    });
  };
  const userIsNew = () => {
    updateProfile({ newFirstSignIn: TIME3 });
  };
  const darActive = () => {
    window.location.pathname = DATA_ACCESS_REQUIREMENTS_PATH;
  };

  test.each([
    ['there is no visible CT', ctNotVisible, () => {}],
    ['the user is not CT eligible', userIneligible, () => {}],
    ['the user has CT access already', ctAccess, () => {}],
    ['the user is too new', userIsNew, () => {}],
    ['the user is currently visiting the DAR', () => {}, darActive],
  ])(
    'should not render if all of the requirements are met, except that %s',
    (desc, preMountModifier, postMountModifier) => {
      fulfillAllBannerRequirements();

      preMountModifier();

      const wrapper = component();
      expect(wrapper.exists()).toBeTruthy();

      postMountModifier();
      waitOneTickAndUpdate(wrapper);

      expect(ctBannerExists(wrapper)).toBeFalsy();
    }
  );
});
