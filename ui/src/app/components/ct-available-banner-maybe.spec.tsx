import '@testing-library/jest-dom';

import * as React from 'react';
import { MemoryRouter } from 'react-router-dom';

import { CdrVersionTier, Profile, UserTierEligibility } from 'generated/fetch';

import { fireEvent, render, screen, within } from '@testing-library/react';
import { AccessTierShortNames } from 'app/utils/access-tiers';
import { DATA_ACCESS_REQUIREMENTS_PATH } from 'app/utils/access-utils';
import { cdrVersionStore, profileStore } from 'app/utils/stores';

import { cdrVersionTiersResponse } from 'testing/stubs/cdr-versions-api-stub';
import { ProfileStubVariables } from 'testing/stubs/profile-api-stub';

import { CTAvailableBannerMaybe } from './ct-available-banner-maybe';

// 3 times, in order
const [TIME1, TIME2, TIME3] = [10000, 20000, 30000];

describe('CTAvailableBannerMaybe', () => {
  const load = jest.fn();
  const reload = jest.fn();
  const updateCache = jest.fn();

  const component = (path: string = '/') => {
    return render(
      <MemoryRouter initialEntries={[path]}>
        <CTAvailableBannerMaybe />
      </MemoryRouter>
    );
  };

  const ctBannerExists = async () =>
    screen.findByTestId('controlled-tier-available');

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

  it('should render if all of the requirements are met', async () => {
    fulfillAllBannerRequirements();

    const { container, getByTestId } = component();
    // console.log('outer container:', exists);
    expect(await ctBannerExists()).toBeInTheDocument();
  });

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
  const darActive = () => component(DATA_ACCESS_REQUIREMENTS_PATH);

  test.each([
    ['the user is not CT eligible', userIneligible, component],
    ['the user has CT access already', ctAccess, component],
    ['the user is too new', userIsNew, component],
    ['the user is currently visiting the DAR', () => {}, darActive],
  ])(
    'should not render if all of the requirements are met, except that %s',
    async (desc, preMountModifier, initWrapper) => {
      fulfillAllBannerRequirements();

      preMountModifier();

      const { container } = initWrapper();
      await expect(ctBannerExists()).rejects.toThrow();
    }
  );
});
