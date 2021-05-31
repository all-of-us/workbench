import {mount} from 'enzyme';
import * as React from 'react';
import * as fp from 'lodash/fp';

import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {profileStore, serverConfigStore} from 'app/utils/stores';
import {InstitutionApi, ProfileApi} from 'generated/fetch';
import {waitOneTickAndUpdate} from 'testing/react-test-helpers';
import SpyInstance = jest.SpyInstance;
import {InstitutionApiStub} from 'testing/stubs/institution-api-stub';
import {ProfileApiStub} from 'testing/stubs/profile-api-stub';
import {ProfileStubVariables} from 'testing/stubs/profile-api-stub';
import {AccessRenewalPage} from 'app/pages/access/access-renewal-page';
import {findNodesByExactText, findNodesContainingText} from 'testing/react-test-helpers';

const EXPIRY_DAYS = 365
const oneYearFromNow = () => Date.now() + 1000 * 60 * 60 * 24 * EXPIRY_DAYS

describe('ProfilePageComponent', () => {

  function expireAllModules() {
    const expiredTime = Date.now() - 1000 * 60 * 60;
    const {profile} = profileStore.get();
    const newProfile = fp.set('renewableAccessModules', {modules: [
      {moduleName: 'dataUseAgreement', expirationEpochMillis: expiredTime},
      {moduleName: 'complianceTraining', expirationEpochMillis: expiredTime},
      {moduleName: 'profileConfirmation', expirationEpochMillis: expiredTime},
      {moduleName: 'publicationConfirmation', expirationEpochMillis: expiredTime}
    ]}, profile)
    profileStore.set({profile: newProfile, load, reload, updateCache});
  }

  function udpateOneModule(updateModuleName, time) {
    const oldProfile = profileStore.get().profile;
    const newModules = fp.map(({moduleName, expirationEpochMillis}) => ({
      moduleName,
      expirationEpochMillis: moduleName === updateModuleName ? time : expirationEpochMillis
    }), oldProfile.renewableAccessModules.modules);
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

  const profile = ProfileStubVariables.PROFILE_STUB;

  let mockUpdateProfile: SpyInstance;

  const component = () => {
    return mount(<AccessRenewalPage/>);
  };

  const load = jest.fn();
  const reload = jest.fn();
  const updateCache = jest.fn();

  beforeEach(() => {
    const profileApi = new ProfileApiStub();

    registerApiClient(ProfileApi, profileApi);
    mockUpdateProfile = jest.spyOn(profileApi, 'updateProfile');

    // mocking because we don't have access to the angular service
    reload.mockImplementation(async() => {
      profileStore.set({profile: profileStore.get().profile, load, reload, updateCache});
    });

    profileStore.set({profile, load, reload, updateCache});

    serverConfigStore.set({config: {
      enableDataUseAgreement: true,
      gsuiteDomain: 'fake-research-aou.org',
      projectId: 'aaa',
      publicApiKeyForErrorReports: 'aaa',
      enableEraCommons: true,
      enableComplianceTraining: true
    }});

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

    udpateOneModule('profileConfirmation', oneYearFromNow());
    await waitOneTickAndUpdate(wrapper);

    expect(findNodesByExactText(wrapper, 'Confirmed').length).toBe(1)
    expect(findNodesByExactText(wrapper, 'Confirm').length).toBe(1);
    expect(findNodesByExactText(wrapper, 'View & Sign').length).toBe(1)
    expect(findNodesByExactText(wrapper, 'Complete Training').length).toBe(1);
  });

  it('should show the correct state when profile and publication are complete', async () => {
    expireAllModules()

    const wrapper = component();

    udpateOneModule('profileConfirmation', oneYearFromNow());
    udpateOneModule('publicationConfirmation', oneYearFromNow());
    await waitOneTickAndUpdate(wrapper);

    expect(findNodesByExactText(wrapper, 'Confirmed').length).toBe(2);
    expect(findNodesByExactText(wrapper, 'View & Sign').length).toBe(1)
    expect(findNodesByExactText(wrapper, 'Complete Training').length).toBe(1);
  });

  it('should show the correct state when all items except DUCC are complete', async () => {
    expireAllModules()

    const wrapper = component();

    udpateOneModule('profileConfirmation', oneYearFromNow());
    udpateOneModule('publicationConfirmation', oneYearFromNow());
    udpateOneModule('complianceTraining', oneYearFromNow());
    await waitOneTickAndUpdate(wrapper);

    expect(findNodesByExactText(wrapper, 'Confirmed').length).toBe(2);
    expect(findNodesByExactText(wrapper, 'View & Sign').length).toBe(1);
    expect(findNodesByExactText(wrapper, 'Completed').length).toBe(1);
  });

  it('should show the correct state when all items are complete', async () => {
    expireAllModules()

    const wrapper = component();

    udpateOneModule('profileConfirmation', oneYearFromNow());
    udpateOneModule('publicationConfirmation', oneYearFromNow());
    udpateOneModule('complianceTraining', oneYearFromNow());
    udpateOneModule('dataUseAgreement', oneYearFromNow());

    setCompletionTimes(() => Date.now());

    await waitOneTickAndUpdate(wrapper);

    expect(findNodesByExactText(wrapper, 'Confirmed').length).toBe(2);
    expect(findNodesByExactText(wrapper, 'Completed').length).toBe(2);
    expect(findNodesContainingText(wrapper, `${EXPIRY_DAYS - 1} days`).length).toBe(4);
    expect(findNodesByExactText(wrapper, 'Thank you for completing all the neccessary steps').length).toBe(1);
  });

});
