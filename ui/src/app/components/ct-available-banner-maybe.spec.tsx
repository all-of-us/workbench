import * as React from 'react';
import {MemoryRouter} from 'react-router-dom';
import {mount} from 'enzyme';

import {CTAvailableBannerMaybe} from './ct-available-banner-maybe';
import {cdrVersionStore, profileStore} from 'app/utils/stores';
import {ProfileStubVariables} from 'testing/stubs/profile-api-stub';
import {AccessTierShortNames} from 'app/utils/access-tiers';
import {environment} from 'environments/environment';
import {
  AccessModule,
  AccessModuleStatus,
  CdrVersionTier,
  Profile,
  UserTierEligibility
} from 'generated/fetch';
import {cdrVersionTiersResponse} from 'testing/stubs/cdr-versions-api-stub';

describe('CTAvailableBannerMaybe', () => {
  const load = jest.fn();
  const reload = jest.fn();
  const updateCache = jest.fn();

  const component = () => {
    return mount(<MemoryRouter><CTAvailableBannerMaybe/></MemoryRouter>);
  };

  const ctBannerExists = (wrapper) => wrapper.find('[data-test-id="controlled-tier-available"]').exists();

  const updateProfile = (newTierEligibilities?: UserTierEligibility[], newUserTiers?: string[], newModuleStatus?: AccessModuleStatus) => {
    const originalProfile = ProfileStubVariables.PROFILE_STUB;

    const unchangedAccessModules: AccessModuleStatus[] =
      originalProfile.accessModules.modules.filter(m => m.moduleName !== newModuleStatus?.moduleName);

    const profile: Profile = {
      ...originalProfile,
      tierEligibilities : newTierEligibilities || originalProfile.tierEligibilities,
      accessTierShortNames: newUserTiers || originalProfile.accessTierShortNames,
      accessModules: {
        modules: [...unchangedAccessModules, newModuleStatus]
      }
    }

    profileStore.set({profile, load, reload, updateCache});
  }

  const updateCdrVersions = (newTier: CdrVersionTier) => {
    const otherTiers = cdrVersionTiersResponse.tiers.filter(tier => tier.accessTierShortName !== tier.accessTierShortName);
    cdrVersionStore.set({tiers: [...otherTiers, newTier]});
  }

  const fulfillAllBannerRequirements = () => {
    // the user is eligible for the CT
    const userEligible: UserTierEligibility[] = [
      {
        accessTierShortName: AccessTierShortNames.Registered,
        eraRequired: false,
        eligible: true
      },
      {
        accessTierShortName: AccessTierShortNames.Controlled,
        eraRequired: false,
        eligible: true
      }
    ];

    // the user does not currently have CT access
    const userTiers: string[] = [AccessTierShortNames.Registered];

    // the environment allows users to see the CT (in the UI)
    environment.accessTiersVisibleToUsers = [AccessTierShortNames.Registered, AccessTierShortNames.Controlled];

    // the user's DUCC access module completion time was before the release of the default CT CDR Version
    const duccStatus: AccessModuleStatus = {
      moduleName: AccessModule.DATAUSERCODEOFCONDUCT,
      completionEpochMillis: 1000
    }
    const controlledTierCdrVersions: CdrVersionTier = {
      ...cdrVersionTiersResponse.tiers.find(t => t.accessTierShortName === AccessTierShortNames.Controlled),
      defaultCdrVersionCreationTime: 2000
    }

    // the user is not currently visiting the DAR page
    window.location.pathname = '/';

    updateProfile(userEligible, userTiers, duccStatus);
    updateCdrVersions(controlledTierCdrVersions);
  }

  beforeEach(() => {
    profileStore.set({profile: ProfileStubVariables.PROFILE_STUB, load, reload, updateCache});
    cdrVersionStore.set(cdrVersionTiersResponse);
  });

  it('should render if all of the requirements are met', () => {
    fulfillAllBannerRequirements();

    const wrapper = component();
    expect(wrapper.exists()).toBeTruthy();

    expect(ctBannerExists(wrapper)).toBeTruthy();
  });

  it('should not render if all of the requirements are met, except that the user is not CT eligible', () => {
    fulfillAllBannerRequirements();

    const userIneligible: UserTierEligibility[] = [
      {
        accessTierShortName: AccessTierShortNames.Registered,
        eraRequired: false,
        eligible: true
      }
    ];

    updateProfile(userIneligible);

    const wrapper = component();
    expect(wrapper.exists()).toBeTruthy();

    expect(ctBannerExists(wrapper)).toBeFalsy();
  });

});
