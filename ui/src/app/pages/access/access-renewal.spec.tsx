import {AccessRenewal} from 'app/pages/access/access-renewal';

import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {profileStore, serverConfigStore} from 'app/utils/stores';
import {mount} from 'enzyme';
import {AccessModule, InstitutionApi, ProfileApi} from 'generated/fetch';
import * as fp from 'lodash/fp';
import * as React from 'react';
import defaultServerConfig from 'testing/default-server-config';
import {findNodesByExactText, findNodesContainingText, waitOneTickAndUpdate} from 'testing/react-test-helpers';
import {InstitutionApiStub} from 'testing/stubs/institution-api-stub';
import {ProfileApiStub, ProfileStubVariables} from 'testing/stubs/profile-api-stub';
import SpyInstance = jest.SpyInstance;

const EXPIRY_DAYS = 365
const oneYearFromNow = () => Date.now() + 1000 * 60 * 60 * 24 * EXPIRY_DAYS
const oneHourAgo = () => Date.now() - 1000 * 60 * 60;

describe('Access Renewal Page', () => {
  const load = jest.fn();
  const reload = jest.fn();
  const updateCache = jest.fn();

  function expireAllModules() {
    const expiredTime = oneHourAgo();
    const {profile} = profileStore.get();

    const newProfile = fp.set('accessModules', {modules: [
      {moduleName: AccessModule.DATAUSERCODEOFCONDUCT, expirationEpochMillis: expiredTime},
      {moduleName: AccessModule.COMPLIANCETRAINING, expirationEpochMillis: expiredTime},
      {moduleName: AccessModule.PROFILECONFIRMATION, expirationEpochMillis: expiredTime},
      {moduleName: AccessModule.PUBLICATIONCONFIRMATION, expirationEpochMillis: expiredTime}
    ]}, profile)
    profileStore.set({profile: newProfile, load, reload, updateCache});
  }

  function updateOneModuleExpirationTime(updateModuleName, time) {
    const oldProfile = profileStore.get().profile;
    const newModules = fp.map(({moduleName, expirationEpochMillis, completionEpochMillis, bypassEpochMillis}) => ({
      moduleName,
      completionEpochMillis: completionEpochMillis,
      bypassEpochMillis: bypassEpochMillis,
      expirationEpochMillis: moduleName === updateModuleName ? time : expirationEpochMillis
    }), oldProfile.accessModules.modules);
    const newProfile = fp.set(['accessModules', 'modules'], newModules, oldProfile)
    profileStore.set({profile: newProfile, load, reload, updateCache});
  }

  function removeOneModule(toBeRemoved) {
    const oldProfile = profileStore.get().profile;
    const newModules = fp.map(({moduleName, expirationEpochMillis, bypassEpochMillis, completionEpochMillis}) =>
        (moduleName === toBeRemoved ? {} : {
          moduleName : moduleName,
          bypassEpochMillis: bypassEpochMillis,
          completionEpochMillis: completionEpochMillis,
          expirationEpochMillis: expirationEpochMillis}),
        oldProfile.accessModules.modules);
    const newProfile = fp.set(['accessModules', 'modules'], newModules, oldProfile)
    profileStore.set({profile: newProfile, load, reload, updateCache});
  }

  function setCompletionTimes(completionFn) {
    const oldProfile = profileStore.get().profile;
    const newModules = fp.map(({moduleName, expirationEpochMillis, bypassEpochMillis}) => ({
      moduleName,
      expirationEpochMillis: expirationEpochMillis,
      bypassEpochMillis: bypassEpochMillis,
      completionEpochMillis: completionFn()
    }), oldProfile.accessModules.modules);
    const newProfile = fp.set(['accessModules', 'modules'], newModules, oldProfile)
    profileStore.set({profile: newProfile,  load, reload, updateCache});
  }

  function setBypassTimes(completionFn) {
    const oldProfile = profileStore.get().profile;
    const newModules = fp.map(({moduleName, expirationEpochMillis, completionEpochMillis}) => ({
      moduleName,
      expirationEpochMillis: expirationEpochMillis,
      // profile and publiction is not bypassable.
      bypassEpochMillis: (moduleName === AccessModule.PROFILECONFIRMATION || moduleName === AccessModule.PUBLICATIONCONFIRMATION)
          ? null
          : completionFn(),
      completionEpochMillis: completionEpochMillis
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

    expect(findNodesContainingText(wrapper, 'access has expired').length).toBe(1);
    expect(findNodesByExactText(wrapper, 'Review').length).toBe(1)
    expect(findNodesByExactText(wrapper, 'Confirm').length).toBe(1)
    expect(findNodesByExactText(wrapper, 'View & Sign').length).toBe(1)
    expect(findNodesByExactText(wrapper, 'Complete Training').length).toBe(1);
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
    removeOneModule('complianceTraining');

    await waitOneTickAndUpdate(wrapper);

    // profileConfirmation, publicationConfirmation, and dataUseAgreement are complete
    expect(findNodesByExactText(wrapper, 'Confirmed').length).toBe(2);
    expect(findNodesByExactText(wrapper, 'Completed').length).toBe(1);
    expect(findNodesContainingText(wrapper, `${EXPIRY_DAYS - 1} days`).length).toBe(3);

    // complianceTraining is not shown because it is disabled
    expect(findNodesByExactText(wrapper, 'Complete Training').length).toBe(0);

    // all of the necessary steps = 3 rather than the usual 4
    expect(findNodesByExactText(wrapper, 'Thank you for completing all the necessary steps').length).toBe(1);
  });

});
