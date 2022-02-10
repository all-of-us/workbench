import * as React from 'react';
import * as fp from 'lodash/fp';
import { mount } from 'enzyme';

import {
  AccessModule,
  AccessModuleStatus,
  InstitutionApi,
  Profile,
  ProfileApi,
} from 'generated/fetch';

import { AccessRenewal, isExpiring } from 'app/pages/access/access-renewal';
import {
  profileApi,
  registerApiClient,
} from 'app/services/swagger-fetch-clients';
import { accessRenewalModules } from 'app/utils/access-utils';
import { nowPlusDays, plusDays } from 'app/utils/dates';
import { profileStore, serverConfigStore } from 'app/utils/stores';

import defaultServerConfig from 'testing/default-server-config';
import {
  findNodesByExactText,
  findNodesContainingText,
  waitOneTickAndUpdate,
} from 'testing/react-test-helpers';
import { InstitutionApiStub } from 'testing/stubs/institution-api-stub';
import {
  ProfileApiStub,
  ProfileStubVariables,
} from 'testing/stubs/profile-api-stub';

const EXPIRY_DAYS = 365;
const oneYearAgo = () => nowPlusDays(-EXPIRY_DAYS);
const oneYearFromNow = () => nowPlusDays(EXPIRY_DAYS);
const oneHourAgo = () => Date.now() - 1000 * 60 * 60;

describe('Access Renewal Page', () => {
  const load = jest.fn();
  const reload = jest.fn();
  const updateCache = jest.fn();

  function expireAllModules() {
    const expiredTime = oneHourAgo();
    const { profile } = profileStore.get();

    const newProfile = fp.set(
      'accessModules',
      {
        modules: accessRenewalModules.map(
          (m) =>
            ({
              moduleName: m,
              completionEpochMillis: expiredTime - 1,
              expirationEpochMillis: expiredTime,
            } as AccessModuleStatus)
        ),
      },
      profile
    );
    profileStore.set({ profile: newProfile, load, reload, updateCache });
  }

  function removeOneModule(toBeRemoved) {
    const oldProfile = profileStore.get().profile;
    const newModules = oldProfile.accessModules.modules.filter(
      (m) => m.moduleName !== toBeRemoved
    );
    const newProfile = fp.set(
      ['accessModules', 'modules'],
      newModules,
      oldProfile
    );
    profileStore.set({ profile: newProfile, load, reload, updateCache });
  }

  function updateOneModuleExpirationTime(updateModuleName, time) {
    const oldProfile = profileStore.get().profile;
    const newModules = [
      ...oldProfile.accessModules.modules.filter(
        (m) => m.moduleName !== updateModuleName
      ),
      {
        ...oldProfile.accessModules.modules.find(
          (m) => m.moduleName === updateModuleName
        ),
        completionEpochMillis: time - 1,
        expirationEpochMillis: time,
      } as AccessModuleStatus,
    ];
    const newProfile = fp.set(
      ['accessModules', 'modules'],
      newModules,
      oldProfile
    );
    profileStore.set({ profile: newProfile, load, reload, updateCache });
  }

  function setCompletionTimes(completionFn) {
    const oldProfile = profileStore.get().profile;
    const newModules = fp.map(
      (moduleStatus) => ({
        ...moduleStatus,
        completionEpochMillis: completionFn(),
      }),
      oldProfile.accessModules.modules
    );
    const newProfile = fp.set(
      ['accessModules', 'modules'],
      newModules,
      oldProfile
    );
    profileStore.set({ profile: newProfile, load, reload, updateCache });
  }

  function setBypassTimes(bypassFn) {
    const oldProfile = profileStore.get().profile;
    const newModules = fp.map(
      (moduleStatus) => ({
        ...moduleStatus,
        bypassEpochMillis:
          // profile and publication are not bypassable.
          moduleStatus.moduleName === AccessModule.PROFILECONFIRMATION ||
          moduleStatus.moduleName === AccessModule.PUBLICATIONCONFIRMATION
            ? null
            : bypassFn(),
      }),
      oldProfile.accessModules.modules
    );
    const newProfile = fp.set(
      ['accessModules', 'modules'],
      newModules,
      oldProfile
    );
    profileStore.set({ profile: newProfile, load, reload, updateCache });
  }

  const expectExpired = (wrapper) =>
    expect(findNodesContainingText(wrapper, 'access has expired').length).toBe(
      1
    );
  const expectNotExpired = (wrapper) =>
    expect(findNodesContainingText(wrapper, 'access has expired').length).toBe(
      0
    );
  const expectComplete = (wrapper) =>
    expect(
      findNodesByExactText(
        wrapper,
        'Thank you for completing all the necessary steps'
      ).length
    ).toBe(1);
  const expectIncomplete = (wrapper) =>
    expect(
      findNodesByExactText(
        wrapper,
        'Thank you for completing all the necessary steps'
      ).length
    ).toBe(0);

  const profile = ProfileStubVariables.PROFILE_STUB;

  const component = () => {
    return mount(<AccessRenewal hideSpinner={() => {}} />);
  };

  beforeEach(() => {
    const profileApiImpl = new ProfileApiStub();

    registerApiClient(ProfileApi, profileApiImpl);

    reload.mockImplementation(async () => {
      profileStore.set({
        profile: profileStore.get().profile,
        load,
        reload,
        updateCache,
      });
    });

    profileStore.set({ profile, load, reload, updateCache });

    serverConfigStore.set({ config: defaultServerConfig });

    const institutionApi = new InstitutionApiStub();
    registerApiClient(InstitutionApi, institutionApi);
  });

  it('should show the correct state when all items are complete', async () => {
    expireAllModules();

    const wrapper = component();

    updateOneModuleExpirationTime(
      AccessModule.PROFILECONFIRMATION,
      oneYearFromNow()
    );
    updateOneModuleExpirationTime(
      AccessModule.PUBLICATIONCONFIRMATION,
      oneYearFromNow()
    );
    updateOneModuleExpirationTime(
      AccessModule.COMPLIANCETRAINING,
      oneYearFromNow()
    );
    updateOneModuleExpirationTime(
      AccessModule.DATAUSERCODEOFCONDUCT,
      oneYearFromNow()
    );

    setCompletionTimes(() => Date.now());

    await waitOneTickAndUpdate(wrapper);

    // All Complete
    expect(findNodesByExactText(wrapper, 'Confirmed').length).toBe(2);
    expect(findNodesByExactText(wrapper, 'Completed').length).toBe(2);
    expect(
      findNodesContainingText(wrapper, `${EXPIRY_DAYS - 1} days`).length
    ).toBe(4);

    expectComplete(wrapper);
    expectNotExpired(wrapper);
  });

  it('should show the correct state when all modules are expired', async () => {
    expireAllModules();
    const wrapper = component();

    setCompletionTimes(oneYearAgo);
    await waitOneTickAndUpdate(wrapper);

    expect(findNodesByExactText(wrapper, 'Review').length).toBe(1);
    expect(findNodesByExactText(wrapper, 'Confirm').length).toBe(1);
    expect(findNodesByExactText(wrapper, 'View & Sign').length).toBe(1);
    expect(findNodesByExactText(wrapper, 'Complete Training').length).toBe(1);

    expectExpired(wrapper);
    expectIncomplete(wrapper);
  });

  it('should show the correct state when all modules are incomplete', async () => {
    const wrapper = component();

    await waitOneTickAndUpdate(wrapper);

    expect(findNodesByExactText(wrapper, 'Review').length).toBe(1);
    expect(findNodesByExactText(wrapper, 'Confirm').length).toBe(1);
    expect(findNodesByExactText(wrapper, 'View & Sign').length).toBe(1);
    expect(findNodesByExactText(wrapper, 'Complete Training').length).toBe(1);

    expectNotExpired(wrapper);
    expectIncomplete(wrapper);
  });

  it('should show the correct state when profile confirmation is complete', async () => {
    expireAllModules();

    const wrapper = component();

    updateOneModuleExpirationTime(
      AccessModule.PROFILECONFIRMATION,
      oneYearFromNow()
    );
    await waitOneTickAndUpdate(wrapper);

    // Complete
    expect(findNodesByExactText(wrapper, 'Confirmed').length).toBe(1);

    // Incomplete
    expect(findNodesByExactText(wrapper, 'Confirm').length).toBe(1);
    expect(findNodesByExactText(wrapper, 'View & Sign').length).toBe(1);
    expect(findNodesByExactText(wrapper, 'Complete Training').length).toBe(1);

    expectExpired(wrapper);
    expectIncomplete(wrapper);
  });

  it('should show the correct state when profile and publication confirmations are complete', async () => {
    expireAllModules();

    const wrapper = component();

    updateOneModuleExpirationTime(
      AccessModule.PROFILECONFIRMATION,
      oneYearFromNow()
    );
    updateOneModuleExpirationTime(
      AccessModule.PUBLICATIONCONFIRMATION,
      oneYearFromNow()
    );
    await waitOneTickAndUpdate(wrapper);

    // Complete
    expect(findNodesByExactText(wrapper, 'Confirmed').length).toBe(2);

    // Incomplete
    expect(findNodesByExactText(wrapper, 'View & Sign').length).toBe(1);
    expect(findNodesByExactText(wrapper, 'Complete Training').length).toBe(1);

    expectExpired(wrapper);
    expectIncomplete(wrapper);
  });

  it('should show the correct state when all items except DUCC are complete', async () => {
    expireAllModules();

    const wrapper = component();

    updateOneModuleExpirationTime(
      AccessModule.PROFILECONFIRMATION,
      oneYearFromNow()
    );
    updateOneModuleExpirationTime(
      AccessModule.PUBLICATIONCONFIRMATION,
      oneYearFromNow()
    );
    updateOneModuleExpirationTime(
      AccessModule.COMPLIANCETRAINING,
      oneYearFromNow()
    );
    await waitOneTickAndUpdate(wrapper);

    // Complete
    expect(findNodesByExactText(wrapper, 'Confirmed').length).toBe(2);
    expect(findNodesByExactText(wrapper, 'Completed').length).toBe(1);
    // Incomplete
    expect(findNodesByExactText(wrapper, 'View & Sign').length).toBe(1);

    expectExpired(wrapper);
    expectIncomplete(wrapper);
  });

  it('should ignore modules which are not expirable', async () => {
    expireAllModules();

    const newModules = [
      ...profileStore.get().profile.accessModules.modules,
      {
        moduleName: AccessModule.TWOFACTORAUTH, // not expirable
        completionEpochMillis: null,
        bypassEpochMillis: null,
        expirationEpochMillis: oneYearAgo(),
      },
    ];

    const newProfile: Profile = {
      ...profileStore.get().profile,
      accessModules: { modules: newModules },
    };

    profileStore.set({ profile: newProfile, load, reload, updateCache });

    const wrapper = component();

    updateOneModuleExpirationTime(
      AccessModule.PROFILECONFIRMATION,
      oneYearFromNow()
    );
    updateOneModuleExpirationTime(
      AccessModule.PUBLICATIONCONFIRMATION,
      oneYearFromNow()
    );
    updateOneModuleExpirationTime(
      AccessModule.COMPLIANCETRAINING,
      oneYearFromNow()
    );
    updateOneModuleExpirationTime(
      AccessModule.DATAUSERCODEOFCONDUCT,
      oneYearFromNow()
    );

    setCompletionTimes(() => Date.now());

    await waitOneTickAndUpdate(wrapper);

    // All Complete
    expect(findNodesByExactText(wrapper, 'Confirmed').length).toBe(2);
    expect(findNodesByExactText(wrapper, 'Completed').length).toBe(2);
    expect(
      findNodesContainingText(wrapper, `${EXPIRY_DAYS - 1} days`).length
    ).toBe(4);

    expectComplete(wrapper);
    expectNotExpired(wrapper);
  });

  it('should show the correct state when items are bypassed', async () => {
    expireAllModules();

    // won't bypass Profile and Publication confirmation because those are unbypassable
    setBypassTimes(() => Date.now());

    const wrapper = component();

    // Incomplete
    expect(findNodesByExactText(wrapper, 'Review').length).toBe(1);
    expect(findNodesByExactText(wrapper, 'Confirm').length).toBe(1);

    // Bypassed
    expect(findNodesByExactText(wrapper, 'Bypassed').length).toBe(2);
    expect(findNodesContainingText(wrapper, '(bypassed)').length).toBe(2);

    // State check
    expect(
      findNodesContainingText(wrapper, 'click the refresh button').length
    ).toBe(0);

    expectExpired(wrapper);
    expectIncomplete(wrapper);
  });

  it('should show the correct state when all items are complete or bypassed', async () => {
    expireAllModules();

    setCompletionTimes(() => Date.now());

    // won't bypass Profile and Publication confirmation because those are unbypassable
    setBypassTimes(() => Date.now());

    updateOneModuleExpirationTime(
      AccessModule.PROFILECONFIRMATION,
      oneYearFromNow()
    );
    updateOneModuleExpirationTime(
      AccessModule.PUBLICATIONCONFIRMATION,
      oneYearFromNow()
    );

    const wrapper = component();

    // Training and DUCC are bypassed
    expect(findNodesByExactText(wrapper, 'Bypassed').length).toBe(2);

    // Publications and Profile are complete
    expect(findNodesByExactText(wrapper, 'Confirmed').length).toBe(2);
    expect(
      findNodesContainingText(wrapper, `${EXPIRY_DAYS - 1} days`).length
    ).toBe(2);

    expectComplete(wrapper);
    expectNotExpired(wrapper);
  });

  it('should show the correct state when modules are disabled', async () => {
    serverConfigStore.set({
      config: {
        ...defaultServerConfig,
        enableComplianceTraining: false,
      },
    });

    const wrapper = component();

    setCompletionTimes(() => Date.now());

    updateOneModuleExpirationTime(
      AccessModule.PROFILECONFIRMATION,
      oneYearFromNow()
    );
    updateOneModuleExpirationTime(
      AccessModule.PUBLICATIONCONFIRMATION,
      oneYearFromNow()
    );
    updateOneModuleExpirationTime(
      AccessModule.DATAUSERCODEOFCONDUCT,
      oneYearFromNow()
    );

    // this module will not be returned in AccessModules because it is disabled
    removeOneModule(AccessModule.COMPLIANCETRAINING);

    await waitOneTickAndUpdate(wrapper);

    // profileConfirmation, publicationConfirmation, and DUCC are complete
    expect(findNodesByExactText(wrapper, 'Confirmed').length).toBe(2);
    expect(findNodesByExactText(wrapper, 'Completed').length).toBe(1);
    expect(
      findNodesContainingText(wrapper, `${EXPIRY_DAYS - 1} days`).length
    ).toBe(3);

    // complianceTraining is not shown because it is disabled
    expect(findNodesByExactText(wrapper, 'Complete Training').length).toBe(0);

    // all of the necessary steps = 3 rather than the usual 4
    expectComplete(wrapper);
    expectNotExpired(wrapper);
  });

  // RW-7473: sync expiring/expired Training module to gain access

  test.each([
    ['should', 'expired', 1, oneHourAgo()],
    ['should', 'expiring', 1, nowPlusDays(10)],
    ['should not', 'complete', 0, oneYearFromNow()],
    ['should not', 'incomplete', 0, null],
  ])(
    '%s externally sync %s Compliance Training module status',
    async (desc1, desc2, expected, expirationTime) => {
      const spy = jest.spyOn(profileApi(), 'syncComplianceTrainingStatus');

      updateOneModuleExpirationTime(
        AccessModule.COMPLIANCETRAINING,
        expirationTime
      );

      const wrapper = component();
      await waitOneTickAndUpdate(wrapper);
      expect(spy).toHaveBeenCalledTimes(expected);
    }
  );
});

describe('isExpiring', () => {
  const ONE_MINUTE_IN_MILLIS = 1000 * 60;
  const LOOKBACK_PERIOD = 99; // arbitrary for testing; actual prod value is 330
  const EXPIRATION_DAYS = 123; // arbitrary for testing; actual prod value is 365

  beforeEach(() => {
    serverConfigStore.set({
      config: {
        ...defaultServerConfig,
        accessRenewalLookback: LOOKBACK_PERIOD,
      },
    });
  });

  it('should return isExpiring=true if a module has expired', () => {
    const completionDaysPast = 555; // arbitrary for test; completed this many days ago

    // add 1 minute so we don't hit the boundary *exactly*
    const completionDate =
      nowPlusDays(-completionDaysPast) + ONE_MINUTE_IN_MILLIS;
    const expirationDate = plusDays(completionDate, EXPIRATION_DAYS);

    // sanity-check: this test is checking an expiration in the past
    expect(expirationDate).toBeLessThan(Date.now());

    expect(isExpiring(expirationDate)).toEqual(true);
  });

  it('should return isExpiring=true if a module will expire in the future and within the lookback period', () => {
    const completionDaysPast = 44; // arbitrary for test; completed this many days ago

    // add 1 minute so we don't hit the boundary *exactly*
    const completionDate =
      nowPlusDays(-completionDaysPast) + ONE_MINUTE_IN_MILLIS;
    const expirationDate = plusDays(completionDate, EXPIRATION_DAYS);

    // sanity-check: this test is checking a date within the lookback
    const endOfLookback = nowPlusDays(LOOKBACK_PERIOD);
    expect(expirationDate).toBeLessThan(endOfLookback);

    expect(isExpiring(expirationDate)).toEqual(true);
  });

  it('should return isExpiring=false if a module will expire in the future beyond the lookback period', () => {
    const completionDaysPast = 3; // arbitrary for test; completed this many days ago

    // add 1 minute so we don't hit the boundary *exactly*
    const completionDate =
      nowPlusDays(-completionDaysPast) + ONE_MINUTE_IN_MILLIS;
    const expirationDate = plusDays(completionDate, EXPIRATION_DAYS);

    // sanity-check: this test is checking a date beyond the lookback
    const endOfLookback = nowPlusDays(LOOKBACK_PERIOD);
    expect(expirationDate).toBeGreaterThan(endOfLookback);

    expect(isExpiring(expirationDate)).toEqual(false);
  });

  it('should return isExpiring=false if a module has a null expiration', () => {
    expect(isExpiring(null)).toEqual(false);
  });

  it('should return isExpiring=false if a module has an undefined expiration', () => {
    expect(isExpiring(undefined)).toEqual(false);
  });
});
