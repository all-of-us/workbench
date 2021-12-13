import * as fp from 'lodash/fp';
import * as React from 'react';
import {mount} from 'enzyme';
import SpyInstance = jest.SpyInstance;

import {AccessRenewal} from 'app/pages/access/access-renewal';
import {profileApi, registerApiClient} from 'app/services/swagger-fetch-clients';
import {profileStore, serverConfigStore} from 'app/utils/stores';
import {AccessModule, InstitutionApi, Profile, ProfileApi} from 'generated/fetch';
import defaultServerConfig from 'testing/default-server-config';
import {findNodesByExactText, findNodesContainingText, waitOneTickAndUpdate} from 'testing/react-test-helpers';
import {InstitutionApiStub} from 'testing/stubs/institution-api-stub';
import {ProfileApiStub, ProfileStubVariables} from 'testing/stubs/profile-api-stub';
import {accessRenewalModules} from 'app/utils/access-utils';

const EXPIRY_DAYS = 365
const addDaysFromNow = (days: number) => Date.now() + 1000 * 60 * 60 * 24 * days;
const oneYearFromNow = () => addDaysFromNow(EXPIRY_DAYS);
const oneHourAgo = () => Date.now() - 1000 * 60 * 60;

describe('Access Renewal Page', () => {
  const load = jest.fn();
  const reload = jest.fn();
  const updateCache = jest.fn();

  function expireAllModules() {
    const expiredTime = oneHourAgo();
    const {profile} = profileStore.get();

    const newProfile = fp.set('accessModules',
        {modules: accessRenewalModules.map(m => ({moduleName: m, expirationEpochMillis: expiredTime}))},
        profile)
    profileStore.set({profile: newProfile, load, reload, updateCache});
  }

  function removeOneModule(toBeRemoved) {
    const oldProfile = profileStore.get().profile;
    const newModules = oldProfile.accessModules.modules.filter(m => m.moduleName !== toBeRemoved);
    const newProfile = fp.set(['accessModules', 'modules'], newModules, oldProfile)
    profileStore.set({profile: newProfile, load, reload, updateCache});
  }

  function updateOneModuleExpirationTime(updateModuleName, time) {
    const oldProfile = profileStore.get().profile;
    const newModules = [
      ...oldProfile.accessModules.modules.filter(m => m.moduleName !== updateModuleName),
      {
        ...oldProfile.accessModules.modules.find(m => m.moduleName === updateModuleName),
        expirationEpochMillis: time
      }];
    const newProfile = fp.set(['accessModules', 'modules'], newModules, oldProfile)
    profileStore.set({profile: newProfile, load, reload, updateCache});
  }

  function setCompletionTimes(completionFn) {
    const oldProfile = profileStore.get().profile;
    const newModules = fp.map((moduleStatus) => ({
      ...moduleStatus,
      completionEpochMillis: completionFn()
    }), oldProfile.accessModules.modules);
    const newProfile = fp.set(['accessModules', 'modules'], newModules, oldProfile)
    profileStore.set({profile: newProfile,  load, reload, updateCache});
  }

  function setBypassTimes(bypassFn) {
    const oldProfile = profileStore.get().profile;
    const newModules = fp.map((moduleStatus) => ({
      ...moduleStatus,
      bypassEpochMillis:
          // profile and publication are not bypassable.
          (moduleStatus.moduleName === AccessModule.PROFILECONFIRMATION || moduleStatus.moduleName === AccessModule.PUBLICATIONCONFIRMATION)
          ? null
          : bypassFn(),
    }), oldProfile.accessModules.modules);
    const newProfile = fp.set(['accessModules', 'modules'], newModules, oldProfile)
    profileStore.set({profile: newProfile,  load, reload, updateCache});
  }

  const profile = ProfileStubVariables.PROFILE_STUB;

  let mockUpdateProfile: SpyInstance;

  const component = () => {
    return mount(<AccessRenewal hideSpinner={() => {}}/>);
  };

  beforeEach(() => {
    const profileApi = new ProfileApiStub();

    registerApiClient(ProfileApi, profileApi);
    mockUpdateProfile = jest.spyOn(profileApi, 'updateProfile');

    reload.mockImplementation(async() => {
      profileStore.set({profile: profileStore.get().profile, load, reload, updateCache});
    });

    profileStore.set({profile, load, reload, updateCache});

    serverConfigStore.set({config: defaultServerConfig});

    const institutionApi = new InstitutionApiStub();
    registerApiClient(InstitutionApi, institutionApi);
  });


  it('should render when the user is expired', async () => {
    expireAllModules()
    const wrapper = component();

    setCompletionTimes(() => Date.now() - 1000 * 60 * 60 * 24 * EXPIRY_DAYS);
    await waitOneTickAndUpdate(wrapper);

    expect(findNodesByExactText(wrapper, 'Review').length).toBe(1)
    expect(findNodesByExactText(wrapper, 'Confirm').length).toBe(1)
    expect(findNodesByExactText(wrapper, 'View & Sign').length).toBe(1)
    expect(findNodesByExactText(wrapper, 'Complete Training').length).toBe(1);

    expect(findNodesContainingText(wrapper, 'access has expired').length).toBe(1);
    expect(findNodesByExactText(wrapper, 'Thank you for completing all the necessary steps').length).toBe(0);
  });

  it('should show the correct state when profile is complete', async () => {
    expireAllModules()

    const wrapper = component();

    updateOneModuleExpirationTime(AccessModule.PROFILECONFIRMATION, oneYearFromNow());
    await waitOneTickAndUpdate(wrapper);

    // Complete
    expect(findNodesByExactText(wrapper, 'Confirmed').length).toBe(1)

    // Incomplete
    expect(findNodesByExactText(wrapper, 'Confirm').length).toBe(1);
    expect(findNodesByExactText(wrapper, 'View & Sign').length).toBe(1)
    expect(findNodesByExactText(wrapper, 'Complete Training').length).toBe(1);

    expect(findNodesContainingText(wrapper, 'access has expired').length).toBe(1);
    expect(findNodesByExactText(wrapper, 'Thank you for completing all the necessary steps').length).toBe(0);
  });

  it('should show the correct state when profile and publication are complete', async () => {
    expireAllModules()

    const wrapper = component();

    updateOneModuleExpirationTime(AccessModule.PROFILECONFIRMATION, oneYearFromNow());
    updateOneModuleExpirationTime(AccessModule.PUBLICATIONCONFIRMATION, oneYearFromNow());
    await waitOneTickAndUpdate(wrapper);

    // Complete
    expect(findNodesByExactText(wrapper, 'Confirmed').length).toBe(2);

    // Incomplete
    expect(findNodesByExactText(wrapper, 'View & Sign').length).toBe(1)
    expect(findNodesByExactText(wrapper, 'Complete Training').length).toBe(1);

    expect(findNodesContainingText(wrapper, 'access has expired').length).toBe(1);
    expect(findNodesByExactText(wrapper, 'Thank you for completing all the necessary steps').length).toBe(0);
  });

  it('should show the correct state when all items except DUCC are complete', async () => {
    expireAllModules()

    const wrapper = component();

    updateOneModuleExpirationTime(AccessModule.PROFILECONFIRMATION, oneYearFromNow());
    updateOneModuleExpirationTime(AccessModule.PUBLICATIONCONFIRMATION, oneYearFromNow());
    updateOneModuleExpirationTime(AccessModule.COMPLIANCETRAINING, oneYearFromNow());
    await waitOneTickAndUpdate(wrapper);

    // Complete
    expect(findNodesByExactText(wrapper, 'Confirmed').length).toBe(2);
    expect(findNodesByExactText(wrapper, 'Completed').length).toBe(1);
    // Incomplete
    expect(findNodesByExactText(wrapper, 'View & Sign').length).toBe(1);

    expect(findNodesContainingText(wrapper, 'access has expired').length).toBe(1);
    expect(findNodesByExactText(wrapper, 'Thank you for completing all the necessary steps').length).toBe(0);
  });

  it('should show the correct state when all items are complete', async () => {
    expireAllModules()

    const wrapper = component();

    updateOneModuleExpirationTime(AccessModule.PROFILECONFIRMATION, oneYearFromNow());
    updateOneModuleExpirationTime(AccessModule.PUBLICATIONCONFIRMATION, oneYearFromNow());
    updateOneModuleExpirationTime(AccessModule.COMPLIANCETRAINING, oneYearFromNow());
    updateOneModuleExpirationTime(AccessModule.DATAUSERCODEOFCONDUCT, oneYearFromNow());

    setCompletionTimes(() => Date.now());

    await waitOneTickAndUpdate(wrapper);

    // All Complete
    expect(findNodesByExactText(wrapper, 'Confirmed').length).toBe(2);
    expect(findNodesByExactText(wrapper, 'Completed').length).toBe(2);
    expect(findNodesContainingText(wrapper, `${EXPIRY_DAYS - 1} days`).length).toBe(4);

    expect(findNodesByExactText(wrapper, 'Thank you for completing all the necessary steps').length).toBe(1);
    expect(findNodesContainingText(wrapper, 'access has expired').length).toBe(0);
  });

  it('should ignore modules which are not expirable', async () => {
    expireAllModules();

    const newModules = [
        ...profileStore.get().profile.accessModules.modules,
        {
          moduleName: AccessModule.TWOFACTORAUTH,   // not expirable
          completionEpochMillis: null,
          bypassEpochMillis: null,
          expirationEpochMillis: null,
        }
    ];

    const newProfile: Profile = {
      ...profileStore.get().profile,
      accessModules: {modules: newModules}
    }

    profileStore.set({profile: newProfile, load, reload, updateCache});

    const wrapper = component();

    updateOneModuleExpirationTime(AccessModule.PROFILECONFIRMATION, oneYearFromNow());
    updateOneModuleExpirationTime(AccessModule.PUBLICATIONCONFIRMATION, oneYearFromNow());
    updateOneModuleExpirationTime(AccessModule.COMPLIANCETRAINING, oneYearFromNow());
    updateOneModuleExpirationTime(AccessModule.DATAUSERCODEOFCONDUCT, oneYearFromNow());

    setCompletionTimes(() => Date.now());

    await waitOneTickAndUpdate(wrapper);

    // All Complete
    expect(findNodesByExactText(wrapper, 'Confirmed').length).toBe(2);
    expect(findNodesByExactText(wrapper, 'Completed').length).toBe(2);
    expect(findNodesContainingText(wrapper, `${EXPIRY_DAYS - 1} days`).length).toBe(4);

    expect(findNodesByExactText(wrapper, 'Thank you for completing all the necessary steps').length).toBe(1);
    expect(findNodesContainingText(wrapper, 'access has expired').length).toBe(0);
  });

  it('should show the correct state when items are bypassed', async () => {
    expireAllModules()

    const wrapper = component();

    setCompletionTimes(() => Date.now());
    setBypassTimes(() => Date.now());

    updateOneModuleExpirationTime(AccessModule.PROFILECONFIRMATION, oneHourAgo());
    updateOneModuleExpirationTime(AccessModule.PUBLICATIONCONFIRMATION, oneHourAgo());

    await waitOneTickAndUpdate(wrapper);

    // Incomplete
    expect(findNodesByExactText(wrapper, 'Review').length).toBe(1)
    expect(findNodesByExactText(wrapper, 'Confirm').length).toBe(1);

    // Bypassed
    expect(findNodesByExactText(wrapper, 'Bypassed').length).toBe(2);
    expect(findNodesContainingText(wrapper, '(bypassed)').length).toBe(2);

    // State check
    expect(findNodesContainingText(wrapper, 'click the refresh button').length).toBe(0);

    expect(findNodesContainingText(wrapper, 'access has expired').length).toBe(1);
    expect(findNodesByExactText(wrapper, 'Thank you for completing all the necessary steps').length).toBe(0);
  });

  it('should show the correct state when all items are complete or bypassed', async () => {
    expireAllModules()

    const wrapper = component();

    updateOneModuleExpirationTime(AccessModule.PROFILECONFIRMATION, oneYearFromNow());
    updateOneModuleExpirationTime(AccessModule.PUBLICATIONCONFIRMATION, oneYearFromNow());

    setBypassTimes(oneYearFromNow);

    setCompletionTimes(oneYearFromNow);

    await waitOneTickAndUpdate(wrapper);

    // Training and DUCC are bypassed
    expect(findNodesByExactText(wrapper, 'Bypassed').length).toBe(2);

    // Publications and Profile are complete
    expect(findNodesByExactText(wrapper, 'Confirmed').length).toBe(2);
    expect(findNodesContainingText(wrapper, `${EXPIRY_DAYS - 1} days`).length).toBe(2);

    expect(findNodesByExactText(wrapper, 'Thank you for completing all the necessary steps').length).toBe(1);
    expect(findNodesContainingText(wrapper, 'access has expired').length).toBe(0);
  });

  it('should show the correct state when modules are disabled', async () => {
    serverConfigStore.set({config: {
      ...defaultServerConfig,
        enableComplianceTraining: false
    }});

    const wrapper = component();

    setCompletionTimes(() => Date.now());

    updateOneModuleExpirationTime(AccessModule.PROFILECONFIRMATION, oneYearFromNow());
    updateOneModuleExpirationTime(AccessModule.PUBLICATIONCONFIRMATION, oneYearFromNow());
    updateOneModuleExpirationTime(AccessModule.DATAUSERCODEOFCONDUCT, oneYearFromNow());

    // these modules will not be returned in AccessModules because they are disabled
    removeOneModule(AccessModule.COMPLIANCETRAINING);

    await waitOneTickAndUpdate(wrapper);

    // profileConfirmation, publicationConfirmation, and DUCC are complete
    expect(findNodesByExactText(wrapper, 'Confirmed').length).toBe(2);
    expect(findNodesByExactText(wrapper, 'Completed').length).toBe(1);
    expect(findNodesContainingText(wrapper, `${EXPIRY_DAYS - 1} days`).length).toBe(3);

    // complianceTraining is not shown because it is disabled
    expect(findNodesByExactText(wrapper, 'Complete Training').length).toBe(0);

    // all of the necessary steps = 3 rather than the usual 4
    expect(findNodesByExactText(wrapper, 'Thank you for completing all the necessary steps').length).toBe(1);
    expect(findNodesContainingText(wrapper, 'access has expired').length).toBe(0);
  });

  // RW-7473: sync expiring/expired Training module to gain access

  test.each([
    ['should', 'expired', 1, oneHourAgo()],
    ['should', 'expiring', 1, addDaysFromNow(10)],
    ['should not', 'complete', 0, oneYearFromNow()],
    ['should not', 'incomplete', 0, null],
  ])
  ('%s externally sync %s Compliance Training module status', async(desc1, desc2, expected, expirationTime) => {
    const spy = jest.spyOn(profileApi(), 'syncComplianceTrainingStatus');

    updateOneModuleExpirationTime(AccessModule.COMPLIANCETRAINING, expirationTime);

    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(spy).toHaveBeenCalledTimes(expected);
  });

});
