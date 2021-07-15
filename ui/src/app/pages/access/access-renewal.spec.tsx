import {mount} from 'enzyme';
import * as React from 'react';
import * as fp from 'lodash/fp';

import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {profileStore, serverConfigStore} from 'app/utils/stores';
import {InstitutionApi, ProfileApi} from 'generated/fetch';
import SpyInstance = jest.SpyInstance;
import {InstitutionApiStub} from 'testing/stubs/institution-api-stub';
import {ProfileApiStub} from 'testing/stubs/profile-api-stub';
import {ProfileStubVariables} from 'testing/stubs/profile-api-stub';
import {AccessRenewal} from 'app/pages/access/access-renewal';
import {findNodesByExactText, findNodesContainingText, waitOneTickAndUpdate} from 'testing/react-test-helpers';
import defaultServerConfig from 'testing/default-server-config';

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

    const newProfile = fp.set('renewableAccessModules', {modules: [
      {moduleName: 'dataUseAgreement', expirationEpochMillis: expiredTime},
      {moduleName: 'complianceTraining', expirationEpochMillis: expiredTime},
      {moduleName: 'profileConfirmation', expirationEpochMillis: expiredTime},
      {moduleName: 'publicationConfirmation', expirationEpochMillis: expiredTime}
    ]}, profile)
    profileStore.set({profile: newProfile, load, reload, updateCache});
  }

  function updateOneModule(updateModuleName, time) {
    const oldProfile = profileStore.get().profile;
    const newModules = fp.map(({moduleName, expirationEpochMillis}) => ({
      moduleName,
      expirationEpochMillis: moduleName === updateModuleName ? time : expirationEpochMillis
    }), oldProfile.renewableAccessModules.modules);
    const newProfile = fp.set(['renewableAccessModules', 'modules'], newModules, oldProfile)
    profileStore.set({profile: newProfile, load, reload, updateCache});
  }

  function removeOneModule(toBeRemoved) {
    const oldProfile = profileStore.get().profile;
    const newModules = fp.map(({moduleName, expirationEpochMillis}) =>
        (moduleName === toBeRemoved ? {} : {
          moduleName,
          expirationEpochMillis}),
        oldProfile.renewableAccessModules.modules);
    const newProfile = fp.set(['renewableAccessModules', 'modules'], newModules, oldProfile)
    profileStore.set({profile: newProfile, load, reload, updateCache});
  }

  function setCompletionTimes(completionFn) {
    const {profile} = profileStore.get();
    const newProfile = fp.flow(
      fp.set('complianceTrainingCompletionTime', completionFn()),
      fp.set('dataUseAgreementCompletionTime', completionFn()),
      fp.set('publicationsLastConfirmedTime', completionFn()),
      fp.set('profileLastConfirmedTime', completionFn())
    )(profile)
    profileStore.set({profile: newProfile,  load, reload, updateCache});
  }

  function setBypassTimes(completionFn) {
    const {profile} = profileStore.get();
    const newProfile = fp.flow(
      fp.set('dataUseAgreementBypassTime', completionFn()),
      fp.set('complianceTrainingBypassTime', completionFn())
    )(profile)
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

    updateOneModule('profileConfirmation', oneYearFromNow());
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

    updateOneModule('profileConfirmation', oneYearFromNow());
    updateOneModule('publicationConfirmation', oneYearFromNow());
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

    updateOneModule('profileConfirmation', oneYearFromNow());
    updateOneModule('publicationConfirmation', oneYearFromNow());
    updateOneModule('complianceTraining', oneYearFromNow());
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

    updateOneModule('profileConfirmation', oneYearFromNow());
    updateOneModule('publicationConfirmation', oneYearFromNow());
    updateOneModule('complianceTraining', oneYearFromNow());
    updateOneModule('dataUseAgreement', oneYearFromNow());

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

    updateOneModule('profileConfirmation', oneHourAgo());
    updateOneModule('publicationConfirmation', oneHourAgo());

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

    updateOneModule('profileConfirmation', oneYearFromNow());
    updateOneModule('publicationConfirmation', oneYearFromNow());

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

    updateOneModule('profileConfirmation', oneYearFromNow());
    updateOneModule('publicationConfirmation', oneYearFromNow());
    updateOneModule('dataUseAgreement', oneYearFromNow());

    // these modules will not be returned in RenewableAccessModules because they are disabled
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
