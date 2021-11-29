import * as React from 'react';
import {mount} from 'enzyme';

import defaultServerConfig from 'testing/default-server-config';
import {AccessModule, InstitutionApi, Profile, ProfileApi} from 'generated/fetch';
import {
    allModules,
    DataAccessRequirements,
    getActiveModule,
    getEligibleModules,
    requiredModules
} from './data-access-requirements';
import {InstitutionApiStub} from 'testing/stubs/institution-api-stub';
import {ProfileApiStub, ProfileStubVariables} from 'testing/stubs/profile-api-stub';
import {profileApi, registerApiClient} from 'app/services/swagger-fetch-clients';
import {profileStore, serverConfigStore} from 'app/utils/stores';
import {MemoryRouter} from 'react-router-dom';
import {waitForFakeTimersAndUpdate, waitOneTickAndUpdate} from 'testing/react-test-helpers';
import {AccessTierShortNames} from 'app/utils/access-tiers';
import {environment} from 'environments/environment';


const profile = ProfileStubVariables.PROFILE_STUB as Profile;
const load = jest.fn();
const reload = jest.fn();
const updateCache = jest.fn();

describe('DataAccessRequirements', () => {
    const component = () => {
        return mount(<MemoryRouter>
            <DataAccessRequirements hideSpinner={() => {}} showSpinner={() => {}}/>
        </MemoryRouter>);
    };

    const findModule = (wrapper, module: AccessModule) => wrapper.find(`[data-test-id="module-${module}"]`);
    const findIneligibleModule = (wrapper, module: AccessModule) => wrapper.find(`[data-test-id="module-${module}-ineligible"]`);
    const findCompleteModule = (wrapper, module: AccessModule) => wrapper.find(`[data-test-id="module-${module}-complete"]`);
    const findIncompleteModule = (wrapper, module: AccessModule) => wrapper.find(`[data-test-id="module-${module}-incomplete"]`);

    const findNextCtaForModule = (wrapper, module: AccessModule) => findModule(wrapper, module).find('[data-test-id="next-module-cta"]');

    const findCompletionBanner = (wrapper) => wrapper.find('[data-test-id="dar-completed"]');

    const findControlledSignedStepEligible = (wrapper) => wrapper.find('[data-test-id="controlled-signed"]')
      .find('[data-test-id="eligible"]');
    const findControlledSignedStepIneligible = (wrapper) => wrapper.find('[data-test-id="controlled-signed"]')
      .find('[data-test-id="ineligible"]');

    const findControlledUserEligible = (wrapper) => wrapper.find('[data-test-id="controlled-user-email"]')
      .find('[data-test-id="eligible"]');
    const findControlledUserIneligible = (wrapper) => wrapper.find('[data-test-id="controlled-user-email"]')
      .find('[data-test-id="ineligible"]');

    const findControlledTierCard = (wrapper) => wrapper.find('[data-test-id="controlled-card"]')
    const findEligibleText = (wrapper) => wrapper.find('[data-test-id="eligible-text"]');
    const findIneligibleText = (wrapper) => wrapper.find('[data-test-id="ineligible-text"]');
    const findClickableModuleText = (wrapper, module: AccessModule) => wrapper.find(`[data-test-id="module-${module}-clickable-text"]`);

    const findContactUs = (wrapper) => wrapper.find('[data-test-id="contact-us"]');

    beforeEach(async() => {
        registerApiClient(InstitutionApi, new InstitutionApiStub());
        registerApiClient(ProfileApi, new ProfileApiStub());

        serverConfigStore.set({config: {...defaultServerConfig, unsafeAllowSelfBypass: true}});
        profileStore.set({profile, load, reload, updateCache});
    });

    afterEach(() => {
        // reset to standard behavior after tests which use fake timers
        jest.useRealTimers();
    })


    it('should return all required modules from getEligibleModules by default (all FFs enabled)', () => {
        const enabledModules = getEligibleModules(allModules, profile);
        requiredModules.forEach(module => expect(enabledModules.includes(module)).toBeTruthy());
    });

    it('should not return the RAS module from getEligibleModules when its feature flag is disabled', () => {
        serverConfigStore.set({config: {...defaultServerConfig, enableRasLoginGovLinking: false, enforceRasLoginGovLinking: false}});
        const enabledModules = getEligibleModules(allModules, profile);
        expect(enabledModules.includes(AccessModule.RASLINKLOGINGOV)).toBeFalsy();
    });

    it('should return the RAS module from getEligibleModules when ' +
        'enforceRasLoginGovLinking is enabled, enableRasLoginGovLinking is not', () => {
        serverConfigStore.set({config: {...defaultServerConfig, enableRasLoginGovLinking: false, enforceRasLoginGovLinking: true}});
        const enabledModules = getEligibleModules(allModules, profile);
        expect(enabledModules.includes(AccessModule.RASLINKLOGINGOV)).toBeTruthy();
    });

    it('should not return the ERA module from getEligibleModules when its feature flag is disabled', () => {
        serverConfigStore.set({config: {...defaultServerConfig, enableEraCommons: false}});
        const enabledModules = getEligibleModules(allModules, profile);
        expect(enabledModules.includes(AccessModule.ERACOMMONS)).toBeFalsy();
    });

    it('should not return the Compliance module from getEligibleModules when its feature flag is disabled', () => {
        serverConfigStore.set({config: {...defaultServerConfig, enableComplianceTraining: false}});
        const enabledModules = getEligibleModules(allModules, profile);
        expect(enabledModules.includes(AccessModule.COMPLIANCETRAINING)).toBeFalsy();
    });

    it('should return the first module (2FA) from getActiveModule when no modules have been completed', () => {
        const enabledModules = getEligibleModules(requiredModules, profile);
        const activeModule = getActiveModule(enabledModules, profile);

        expect(activeModule).toEqual(requiredModules[0]);
        expect(activeModule).toEqual(enabledModules[0]);

        // update this if the order changes
        expect(activeModule).toEqual(AccessModule.TWOFACTORAUTH)
    });

    it('should return the second module (RAS) from getActiveModule when the first module (2FA) has been completed', () => {
        const testProfile = {
            ...profile,
            accessModules: {
                modules: [{moduleName: AccessModule.TWOFACTORAUTH, completionEpochMillis: 1}]
            }
        };

        const enabledModules = getEligibleModules(requiredModules, profile);
        const activeModule = getActiveModule(enabledModules, testProfile);

        expect(activeModule).toEqual(requiredModules[1]);
        expect(activeModule).toEqual(enabledModules[1]);

        // update this if the order changes
        expect(activeModule).toEqual(AccessModule.RASLINKLOGINGOV)
    });

    it('should return the second module (RAS) from getActiveModule when the first module (2FA) has been bypassed', () => {
        const testProfile = {
            ...profile,
            accessModules: {
                modules: [{moduleName: AccessModule.TWOFACTORAUTH, bypassEpochMillis: 1}]
            }
        };

        const enabledModules = getEligibleModules(requiredModules, profile);
        const activeModule = getActiveModule(enabledModules, testProfile);

        expect(activeModule).toEqual(requiredModules[1]);
        expect(activeModule).toEqual(enabledModules[1]);

        // update this if the order changes
        expect(activeModule).toEqual(AccessModule.RASLINKLOGINGOV)
    });

    it('should return the second enabled module (ERA, not RAS) from getActiveModule' +
      ' when the first module (2FA) has been completed and RAS is disabled', () => {
        serverConfigStore.set({config: {...defaultServerConfig, enableRasLoginGovLinking: false, enforceRasLoginGovLinking: false}});

        const testProfile = {
            ...profile,
            accessModules: {
                modules: [{moduleName: AccessModule.TWOFACTORAUTH, completionEpochMillis: 1}]
            }
        };

        const enabledModules = getEligibleModules(requiredModules, profile);
        const activeModule = getActiveModule(enabledModules, testProfile);

        // update this if the order changes
        expect(activeModule).toEqual(AccessModule.ERACOMMONS)

        // 2FA (module 0) is complete, so enabled #1 is active
        expect(activeModule).toEqual(enabledModules[1]);

        // but we skip requiredModules[1] because it's RAS and is not enabled
        expect(activeModule).toEqual(requiredModules[2]);
    });

    it('should return the fourth module (Compliance) from getActiveModule when the first 3 modules have been completed', () => {
        const testProfile = {
            ...profile,
            accessModules: {
                modules: [
                    {moduleName: AccessModule.TWOFACTORAUTH, completionEpochMillis: 1},
                    {moduleName: AccessModule.ERACOMMONS, completionEpochMillis: 1},
                    {moduleName: AccessModule.RASLINKLOGINGOV, completionEpochMillis: 1},
                ]
            }
        };

        const enabledModules = getEligibleModules(requiredModules, profile);
        const activeModule = getActiveModule(enabledModules, testProfile);

        expect(activeModule).toEqual(requiredModules[3]);
        expect(activeModule).toEqual(enabledModules[3]);

        // update this if the order changes
        expect(activeModule).toEqual(AccessModule.COMPLIANCETRAINING)
    });

    it('should return undefined from getActiveModule when all modules have been completed', () => {
        const testProfile = {
            ...profile,
            accessModules: {
                modules: requiredModules.map(module => ({moduleName: module, completionEpochMillis: 1}))
            }
        };

        const enabledModules = getEligibleModules(requiredModules, profile);
        const activeModule = getActiveModule(enabledModules, testProfile);

        expect(activeModule).toBeUndefined();
    });

    it('should not indicate the RAS module as active when a user has completed it', () => {
        // initially, the user has completed all required modules except RAS (the standard case at RAS launch time)
        const testProfile = {
            ...profile,
            accessModules: {
                modules: [
                    {moduleName: AccessModule.TWOFACTORAUTH, completionEpochMillis: 1},
                    {moduleName: AccessModule.ERACOMMONS, completionEpochMillis: 1},
                    {moduleName: AccessModule.COMPLIANCETRAINING, completionEpochMillis: 1},
                    {moduleName: AccessModule.DATAUSERCODEOFCONDUCT, completionEpochMillis: 1}
                ]
            }
        };

        const enabledModules = getEligibleModules(requiredModules, profile);

        let activeModule = getActiveModule(enabledModules, testProfile);
        expect(activeModule).toEqual(AccessModule.RASLINKLOGINGOV)

        // simulate handleRasCallback() by updating the profile

        const updatedProfile = {
            ...testProfile,
            accessModules: {
                modules: [
                    ...testProfile.accessModules.modules,
                    {moduleName: AccessModule.RASLINKLOGINGOV, completionEpochMillis: 1},
                ]
            }
        };

        activeModule = getActiveModule(enabledModules, updatedProfile);
        expect(activeModule).toBeUndefined();
    });

    it('should render all required modules by default (all FFs enabled)', () => {
        const wrapper = component();
        allModules.forEach(module => expect(findModule(wrapper, module).exists()).toBeTruthy());
    });

    it('should not render the ERA module when its feature flag is disabled', () => {
        serverConfigStore.set({config: {...defaultServerConfig, enableEraCommons: false}});
        const wrapper = component();
        expect(findModule(wrapper, AccessModule.ERACOMMONS).exists()).toBeFalsy();
    });

    it('should not render the Compliance module when its feature flag is disabled', () => {
        serverConfigStore.set({config: {...defaultServerConfig, enableComplianceTraining: false}});
        const wrapper = component();
        expect(findModule(wrapper, AccessModule.COMPLIANCETRAINING).exists()).toBeFalsy();
    });

    // Temporary hack Sep 16: when enableRasLoginGovLinking is false, we DO show the module
    // along with an Ineligible icon and some "technical difficulties" text
    // and it never becomes an activeModule
    it('should render the RAS module as ineligible when its feature flag is disabled', () => {
        serverConfigStore.set({config: {...defaultServerConfig, enableRasLoginGovLinking: false, enforceRasLoginGovLinking: false}});
        const wrapper = component();
        expect(findModule(wrapper, AccessModule.RASLINKLOGINGOV).exists()).toBeTruthy();
        expect(findIneligibleModule(wrapper, AccessModule.RASLINKLOGINGOV).exists()).toBeTruthy();

        expect(findCompleteModule(wrapper, AccessModule.RASLINKLOGINGOV).exists()).toBeFalsy();
        expect(findIncompleteModule(wrapper, AccessModule.RASLINKLOGINGOV).exists()).toBeFalsy();
    });

    it('should render all required modules as incomplete when the profile accessModules are empty', () => {
        const wrapper = component();
        requiredModules.forEach(module => {
            expect(findIncompleteModule(wrapper, module).exists()).toBeTruthy();

            expect(findCompleteModule(wrapper, module).exists()).toBeFalsy();
            expect(findIneligibleModule(wrapper, module).exists()).toBeFalsy();
        });
        expect(findCompletionBanner(wrapper).exists()).toBeFalsy();
    });

    it('should render all required modules as complete when the profile accessModules are all complete', () => {
        profileStore.set({
            profile: {
                ...ProfileStubVariables.PROFILE_STUB,
                accessModules: {
                    modules: requiredModules.map(module => ({moduleName: module, completionEpochMillis: 1}))
                }
            },
            load,
            reload,
            updateCache});

        const wrapper = component();
        requiredModules.forEach(module => {
            expect(findCompleteModule(wrapper, module).exists()).toBeTruthy();

            expect(findIncompleteModule(wrapper, module).exists()).toBeFalsy();
            expect(findIneligibleModule(wrapper, module).exists()).toBeFalsy();
        });
        expect(findCompletionBanner(wrapper).exists()).toBeTruthy();
    });

    // RAS launch bug (no JIRA ticket)
    it('should render all modules as complete by transitioning to all complete', async() => {
        // this test is subject to flakiness using real timers
        jest.useFakeTimers();

        // initially, the user has completed all required modules except RAS (the standard case at RAS launch time)

        const allExceptRas = requiredModules.filter(m => m !== AccessModule.RASLINKLOGINGOV);
        profileStore.set({
            profile: {
                ...ProfileStubVariables.PROFILE_STUB,
                accessModules: {
                    modules: allExceptRas.map(module => ({moduleName: module, completionEpochMillis: 1}))
                }
            },
            load,
            reload,
            updateCache});

        const wrapper = component();
        allExceptRas.forEach(module => {
            expect(findCompleteModule(wrapper, module).exists()).toBeTruthy();

            expect(findIncompleteModule(wrapper, module).exists()).toBeFalsy();
            expect(findIneligibleModule(wrapper, module).exists()).toBeFalsy();
        });

        // RAS is not complete
        expect(findIncompleteModule(wrapper, AccessModule.RASLINKLOGINGOV).exists()).toBeTruthy();

        expect(findCompleteModule(wrapper, AccessModule.RASLINKLOGINGOV).exists()).toBeFalsy();
        expect(findIneligibleModule(wrapper, AccessModule.RASLINKLOGINGOV).exists()).toBeFalsy();

        expect(findCompletionBanner(wrapper).exists()).toBeFalsy();

        // now all required modules are complete

        profileStore.set({
            profile: {
                ...ProfileStubVariables.PROFILE_STUB,
                accessModules: {
                    modules: requiredModules.map(module => ({moduleName: module, completionEpochMillis: 1}))
                }
            },
            load,
            reload,
            updateCache});

        await waitForFakeTimersAndUpdate(wrapper);

        requiredModules.forEach(module => {
            expect(findCompleteModule(wrapper, module).exists()).toBeTruthy();

            expect(findIncompleteModule(wrapper, module).exists()).toBeFalsy();
            expect(findIneligibleModule(wrapper, module).exists()).toBeFalsy();
        });

        expect(findCompletionBanner(wrapper).exists()).toBeTruthy();
    });


    it('should render all required modules as complete when the profile accessModules are all bypassed', () => {
        profileStore.set({
            profile: {
                ...ProfileStubVariables.PROFILE_STUB,
                accessModules: {
                    modules: requiredModules.map(module => {return {moduleName: module, bypassEpochMillis: 1}})
                }
            },
            load,
            reload,
            updateCache});

        const wrapper = component();
        requiredModules.forEach(module => {
            expect(findCompleteModule(wrapper, module).exists()).toBeTruthy();

            expect(findIncompleteModule(wrapper, module).exists()).toBeFalsy();
            expect(findIneligibleModule(wrapper, module).exists()).toBeFalsy();
        });
        expect(findCompletionBanner(wrapper).exists()).toBeTruthy();
    });

    it('should render a mix of complete and incomplete modules, as appropriate', () => {
        const requiredModulesSize = requiredModules.length;
        const incompleteModules = [AccessModule.RASLINKLOGINGOV];
        const completeModules = requiredModules.filter(module => module !== AccessModule.RASLINKLOGINGOV);
        const completeModulesSize = requiredModulesSize - incompleteModules.length;

        // sanity check
        expect(incompleteModules.length).toEqual(1);
        expect(completeModules.length).toEqual(completeModulesSize);

        profileStore.set({
            profile: {
                ...ProfileStubVariables.PROFILE_STUB,
                accessModules: {
                    modules: completeModules.map(module => {return {moduleName: module, completionEpochMillis: 1}})
                }
            },
            load,
            reload,
            updateCache});

        const wrapper = component();
        incompleteModules.forEach(module => {
            expect(findIncompleteModule(wrapper, module).exists()).toBeTruthy();

            expect(findCompleteModule(wrapper, module).exists()).toBeFalsy();
            expect(findIneligibleModule(wrapper, module).exists()).toBeFalsy();
        });
        completeModules.forEach(module => {
            expect(findCompleteModule(wrapper, module).exists()).toBeTruthy();

            expect(findIncompleteModule(wrapper, module).exists()).toBeFalsy();
            expect(findIneligibleModule(wrapper, module).exists()).toBeFalsy();
        });
        expect(findCompletionBanner(wrapper).exists()).toBeFalsy();
    });

    it('should not show self-bypass UI when unsafeSelfBypass is false', () => {
        serverConfigStore.set({config: {...serverConfigStore.get().config, unsafeAllowSelfBypass: false}});
        const wrapper = component();
        expect(wrapper.find('[data-test-id="self-bypass"]').exists()).toBeFalsy();
    });

    it('should show self-bypass when unsafeSelfBypass is true', () => {
        serverConfigStore.set({config: {...serverConfigStore.get().config, unsafeAllowSelfBypass: true}});
        const wrapper = component();
        expect(wrapper.find('[data-test-id="self-bypass"]').exists()).toBeTruthy();
    });

    it('should not show self-bypass UI when all clickable modules are complete', () => {
        serverConfigStore.set({config: {...serverConfigStore.get().config, unsafeAllowSelfBypass: true}});
        profileStore.set({
            profile: {
                ...ProfileStubVariables.PROFILE_STUB,
                accessModules: {
                    modules: requiredModules.map(module => {return {moduleName: module, completionEpochMillis: 1}})
                }
            },
            load,
            reload,
            updateCache});

        const wrapper = component();
        expect(wrapper.find('[data-test-id="self-bypass"]').exists()).toBeFalsy();
    });

    it('should show self-bypass UI when optional modules are still pending', () => {
        serverConfigStore.set({config: {...serverConfigStore.get().config, unsafeAllowSelfBypass: true}});
        profileStore.set({
          profile: {
            ...ProfileStubVariables.PROFILE_STUB,
            accessModules: {
              modules: requiredModules.map(module => {return {moduleName: module, completionEpochMillis: 1}})
            },
            tierEligibilities: [{
              accessTierShortName: AccessTierShortNames.Controlled,
              eraRequired: true,
              eligible: true
            }, {
              accessTierShortName: AccessTierShortNames.Registered,
              eraRequired: false,
              eligible: true
            }]
          },
          load,
          reload,
          updateCache});

        const wrapper = component();
        expect(wrapper.find('[data-test-id="self-bypass"]').exists()).toBeTruthy();
    });

    // regression tests for RW-7384: sync external modules to gain access

    // TODO(RW-7610): Fix timeout flake and re-enable.
    test.skip('should sync incomplete external modules', async() => {
        // profile contains no completed modules, so we sync all (2FA, ERA, Compliance)
        const spy2FA = jest.spyOn(profileApi(), 'syncTwoFactorAuthStatus');
        const spyERA = jest.spyOn(profileApi(), 'syncEraCommonsStatus');
        const spyCompliance = jest.spyOn(profileApi(), 'syncComplianceTrainingStatus');

        const wrapper = component();
        await waitOneTickAndUpdate(wrapper);

        expect(spy2FA).toHaveBeenCalledTimes(1);
        expect(spyERA).toHaveBeenCalledTimes(1);
        expect(spyCompliance).toHaveBeenCalledTimes(1);
     });

    it('should not sync complete external modules', async() => {
        profileStore.set({
            profile: {
                ...ProfileStubVariables.PROFILE_STUB,
                accessModules: {
                    modules: requiredModules.map(module => ({moduleName: module, completionEpochMillis: 1}))
                }
            },
            load,
            reload,
            updateCache});

        const spy2FA = jest.spyOn(profileApi(), 'syncTwoFactorAuthStatus');
        const spyERA = jest.spyOn(profileApi(), 'syncEraCommonsStatus');
        const spyCompliance = jest.spyOn(profileApi(), 'syncComplianceTrainingStatus');

        const wrapper = component();
        await waitOneTickAndUpdate(wrapper);

        expect(spy2FA).toHaveBeenCalledTimes(0);
        expect(spyERA).toHaveBeenCalledTimes(0);
        expect(spyCompliance).toHaveBeenCalledTimes(0);
    });

    it('Should not show Era Commons Module for Registered Tier if the institution does not require eRa', async() => {
        let wrapper = component();
        await waitOneTickAndUpdate(wrapper);
        expect(findModule(wrapper, AccessModule.ERACOMMONS).exists()).toBeTruthy();

        profileStore.set({
            profile: {
                ...ProfileStubVariables.PROFILE_STUB,
                tierEligibilities: [{
                    accessTierShortName: AccessTierShortNames.Registered,
                    eraRequired: false
                }]
            },
            load,
            reload,
            updateCache});
        wrapper = component();
        await waitOneTickAndUpdate(wrapper);
        expect(findModule(wrapper, AccessModule.ERACOMMONS).exists()).toBeFalsy();

        // Ignore eraRequired if the accessTier is Controlled
        profileStore.set({
            profile: {
                ...ProfileStubVariables.PROFILE_STUB,
                tierEligibilities: [{
                    accessTierShortName: AccessTierShortNames.Registered,
                    eraRequired: true
                },{
                    accessTierShortName: AccessTierShortNames.Controlled,
                    eraRequired: false
                }]
            },
            load,
            reload,
            updateCache});
        wrapper = component();
        await waitOneTickAndUpdate(wrapper);
        expect(findModule(wrapper, AccessModule.ERACOMMONS).exists()).toBeTruthy();
    });

    it('Should display Institution has signed agreement when the user has a Tier Eligibility object for CT', async() => {
        let wrapper = component();
        await waitOneTickAndUpdate(wrapper);

        profileStore.set({
            profile: {
                ...ProfileStubVariables.PROFILE_STUB,
                tierEligibilities: [{
                    accessTierShortName: AccessTierShortNames.Controlled,
                    eraRequired: true
                }]
            },
            load,
            reload,
            updateCache
        });
        wrapper = component();
        await waitOneTickAndUpdate(wrapper);
        expect(findControlledSignedStepEligible(wrapper).exists()).toBeTruthy();
        expect(findControlledSignedStepIneligible(wrapper).exists()).toBeFalsy();

        // but this is not enough; the user needs to be made eligible by email as well
        expect(findEligibleText(findControlledTierCard(wrapper)).exists()).toBeFalsy();
        expect(findIneligibleText(findControlledTierCard(wrapper)).exists()).toBeTruthy();
    });

    it('Should not display Institution has signed agreement when the user doesn\'t have a Tier Eligibility object for CT', async() => {
        let wrapper = component();
        await waitOneTickAndUpdate(wrapper);

        profileStore.set({
            profile: {
                ...ProfileStubVariables.PROFILE_STUB,
                tierEligibilities: [{
                    accessTierShortName: AccessTierShortNames.Registered,
                    eraRequired: true
                }]
            },
            load,
            reload,
            updateCache
        });
        wrapper = component();
        await waitOneTickAndUpdate(wrapper);
        expect(findControlledSignedStepEligible(wrapper).exists()).toBeFalsy();
        expect(findControlledSignedStepIneligible(wrapper).exists()).toBeTruthy();

        // but this is not enough; the user needs to be made eligible by email as well
        expect(findEligibleText(findControlledTierCard(wrapper)).exists()).toBeFalsy();
        expect(findIneligibleText(findControlledTierCard(wrapper)).exists()).toBeTruthy();
    });

    it('Should display Institution allows you to access CT when the user\'s CT Tier Eligibility object has eligible=true', async() => {
        let wrapper = component();
        await waitOneTickAndUpdate(wrapper);

        profileStore.set({
            profile: {
                ...ProfileStubVariables.PROFILE_STUB,
                tierEligibilities: [{
                    accessTierShortName: AccessTierShortNames.Controlled,
                    eraRequired: true,
                    eligible: true
                }]
            },
            load,
            reload,
            updateCache
        });
        wrapper = component();
        await waitOneTickAndUpdate(wrapper);
        expect(findControlledUserEligible(wrapper).exists()).toBeTruthy();
        expect(findControlledUserIneligible(wrapper).exists()).toBeFalsy();

        expect(findEligibleText(findControlledTierCard(wrapper)).exists()).toBeTruthy();
        expect(findIneligibleText(findControlledTierCard(wrapper)).exists()).toBeFalsy();
    });

    it('Should not display Institution allows you to access CT when the user\'s CT Tier Eligibility object has eligible=false', async() => {
        let wrapper = component();
        await waitOneTickAndUpdate(wrapper);

        profileStore.set({
            profile: {
                ...ProfileStubVariables.PROFILE_STUB,
                tierEligibilities: [{
                    accessTierShortName: AccessTierShortNames.Controlled,
                    eraRequired: true,
                    eligible: false
                }]
            },
            load,
            reload,
            updateCache
        });
        wrapper = component();
        await waitOneTickAndUpdate(wrapper);
        expect(findControlledUserEligible(wrapper).exists()).toBeFalsy();
        expect(findControlledUserIneligible(wrapper).exists()).toBeTruthy();

        expect(findEligibleText(findControlledTierCard(wrapper)).exists()).toBeFalsy();
        expect(findIneligibleText(findControlledTierCard(wrapper)).exists()).toBeTruthy();
    });

    it('Should not display Institution allows you to access CT when the user does not have a CT Tier Eligibility object', async() => {
        let wrapper = component();
        await waitOneTickAndUpdate(wrapper);

        profileStore.set({
            profile: {
                ...ProfileStubVariables.PROFILE_STUB,
                // no CT eligibility object
                tierEligibilities: [{
                    accessTierShortName: AccessTierShortNames.Registered,
                    eraRequired: true,
                    eligible: false
                }]
            },
            load,
            reload,
            updateCache
        });
        wrapper = component();
        await waitOneTickAndUpdate(wrapper);
        expect(findControlledUserEligible(wrapper).exists()).toBeFalsy();
        expect(findControlledUserIneligible(wrapper).exists()).toBeTruthy();

        expect(findEligibleText(findControlledTierCard(wrapper)).exists()).toBeFalsy();
        expect(findIneligibleText(findControlledTierCard(wrapper)).exists()).toBeTruthy();
    });

    it('Should display the CT card when the environment has a Controlled Tier', async() => {
        environment.accessTiersVisibleToUsers = [AccessTierShortNames.Registered, AccessTierShortNames.Controlled];

        let wrapper = component();
        await waitOneTickAndUpdate(wrapper);
        expect(findControlledTierCard(wrapper).exists()).toBeTruthy();
    });

    it('Should not display the CT card when the environment does not have a Controlled Tier', async() => {
        environment.accessTiersVisibleToUsers = [AccessTierShortNames.Registered];

        let wrapper = component();
        await waitOneTickAndUpdate(wrapper);
        expect(findControlledTierCard(wrapper).exists()).toBeFalsy();
    });


    it('Should display eraCommons module in CT card ' +
        'when the user institution has signed agreement and CT requires eraCommons and RT does not', async() => {
        environment.accessTiersVisibleToUsers = [AccessTierShortNames.Registered, AccessTierShortNames.Controlled];
        let wrapper = component();
        await waitOneTickAndUpdate(wrapper);

        profileStore.set({
            profile: {
                ...ProfileStubVariables.PROFILE_STUB,
                tierEligibilities: [{
                    accessTierShortName: AccessTierShortNames.Controlled,
                    eraRequired: true,
                    eligible: true
                },{
                    accessTierShortName: AccessTierShortNames.Registered,
                    eraRequired: false,
                    eligible: false
                },]
            },
            load,
            reload,
            updateCache
        });
        wrapper = component();
        await waitOneTickAndUpdate(wrapper);
        expect(findModule(findControlledTierCard(wrapper), AccessModule.ERACOMMONS).exists()).toBeTruthy();
    });

    it('Should not display eraCommons module in CT card ' +
        'when RT requires eraCommons', async() => {
        let wrapper = component();
        await waitOneTickAndUpdate(wrapper);

        profileStore.set({
            profile: {
                ...ProfileStubVariables.PROFILE_STUB,
                tierEligibilities: [{
                    accessTierShortName: AccessTierShortNames.Registered,
                    eraRequired: true,
                    eligible: false
                },{
                    accessTierShortName: AccessTierShortNames.Controlled,
                    eraRequired: true,
                    eligible: true
                }]
            },
            load,
            reload,
            updateCache
        });
        wrapper = component();
        await waitOneTickAndUpdate(wrapper);
        expect(findModule(findControlledTierCard(wrapper), AccessModule.ERACOMMONS).exists()).toBeFalsy();
    });

    it('Should not display eraCommons module in CT card ' +
        'when user\'s institution has not signed CT Institution agreement', async() => {
        let wrapper = component();
        await waitOneTickAndUpdate(wrapper);

        profileStore.set({
            profile: {
                ...ProfileStubVariables.PROFILE_STUB,
                // no CT eligibility object
                tierEligibilities: [{
                    accessTierShortName: AccessTierShortNames.Registered,
                    eraRequired: false,
                    eligible: false
                }]
            },
            load,
            reload,
            updateCache
        });
        wrapper = component();
        await waitOneTickAndUpdate(wrapper);
        expect(findModule(findControlledTierCard(wrapper), AccessModule.ERACOMMONS).exists()).toBeFalsy();
    });


    it('Should display ineligible CT Compliance Training module in CT card ' +
        'when user\'s institution has not signed CT Institution agreement', async() => {
        let wrapper = component();
        await waitOneTickAndUpdate(wrapper);

        profileStore.set({
            profile: {
                ...ProfileStubVariables.PROFILE_STUB,
                // no CT eligibility object
                tierEligibilities: [{
                    accessTierShortName: AccessTierShortNames.Registered,
                    eraRequired: false,
                    eligible: false
                }]
            },
            load,
            reload,
            updateCache
        });
        wrapper = component();
        await waitOneTickAndUpdate(wrapper);
        expect(findIneligibleModule(findControlledTierCard(wrapper), AccessModule.CTCOMPLIANCETRAINING).exists()).toBeTruthy();
    });

    it('Should display ineligible CT Compliance Training module in CT card ' +
        'when user is not eligible for CT', async() => {
        let wrapper = component();
        await waitOneTickAndUpdate(wrapper);

        profileStore.set({
            profile: {
                ...ProfileStubVariables.PROFILE_STUB,
                tierEligibilities: [{
                    accessTierShortName: AccessTierShortNames.Registered,
                    eraRequired: false,
                    eligible: true
                }, {
                    accessTierShortName: AccessTierShortNames.Controlled,
                    eraRequired: false,
                    // User not eligible for CT i.e user email doesnt match
                    // Institution's Controlled Tier email list
                    eligible: false
                }]
            },
            load,
            reload,
            updateCache
        });
        wrapper = component();
        await waitOneTickAndUpdate(wrapper);
        expect(findIneligibleModule(findControlledTierCard(wrapper), AccessModule.CTCOMPLIANCETRAINING).exists()).toBeTruthy();
    });

    it('Should not display eraCommons module in CT card ' +
        'when eraCommons is disabled via the environment config', async() => {
        serverConfigStore.set({config: {...defaultServerConfig, enableEraCommons: false}});

        let wrapper = component();
        await waitOneTickAndUpdate(wrapper);

        profileStore.set({
            profile: {
                ...ProfileStubVariables.PROFILE_STUB,
                tierEligibilities: [{
                    accessTierShortName: AccessTierShortNames.Registered,
                    eraRequired: false,
                    eligible: false
                },{
                    accessTierShortName: AccessTierShortNames.Controlled,
                    eraRequired: true,
                    eligible: true
                }]
            },
            load,
            reload,
            updateCache
        });
        wrapper = component();
        await waitOneTickAndUpdate(wrapper);
        expect(findModule(findControlledTierCard(wrapper), AccessModule.ERACOMMONS).exists()).toBeFalsy();
    });

    it('Should show the RAS help text component when it is incomplete', async() => {
        const wrapper = component();
        await waitOneTickAndUpdate(wrapper);

        expect(findCompleteModule(wrapper, AccessModule.RASLINKLOGINGOV).exists()).toBeFalsy();
        expect(findIncompleteModule(wrapper, AccessModule.RASLINKLOGINGOV).exists()).toBeTruthy();

        expect(findContactUs(wrapper).exists()).toBeTruthy();
    });

    it('Should not show the RAS help text component when it is complete', async() => {

        profileStore.set({
            profile: {
                ...ProfileStubVariables.PROFILE_STUB,
                accessModules: {
                    modules: [{moduleName: AccessModule.RASLINKLOGINGOV, completionEpochMillis: 1}]
                }
            },
            load,
            reload,
            updateCache});

        const wrapper = component();
        await waitOneTickAndUpdate(wrapper);

        expect(findCompleteModule(wrapper, AccessModule.RASLINKLOGINGOV).exists()).toBeTruthy();
        expect(findIncompleteModule(wrapper, AccessModule.RASLINKLOGINGOV).exists()).toBeFalsy();

        expect(findContactUs(wrapper).exists()).toBeFalsy();
    });

    it('Should not display ct Compliance Training module in CT card ' +
        'when enableComplianceTraining is false', async() => {
        serverConfigStore.set({config: {...defaultServerConfig, enableComplianceTraining: false}});

        let wrapper = component();
        await waitOneTickAndUpdate(wrapper);

        profileStore.set({
            profile: {
                ...ProfileStubVariables.PROFILE_STUB,
                tierEligibilities: [{
                    accessTierShortName: AccessTierShortNames.Registered,
                    eraRequired: false,
                    eligible: false
                },{
                    accessTierShortName: AccessTierShortNames.Controlled,
                    eraRequired: true,
                    eligible: true
                }]
            },
            load,
            reload,
            updateCache
        });
        wrapper = component();
        await waitOneTickAndUpdate(wrapper);
        expect(findModule(findControlledTierCard(wrapper), AccessModule.CTCOMPLIANCETRAINING).exists()).toBeFalsy();
      });

  it('Should display CT training when ineligible', async() => {
    let wrapper = component();
    await waitOneTickAndUpdate(wrapper);

    profileStore.set({
      profile: {
        ...ProfileStubVariables.PROFILE_STUB,
        tierEligibilities: [{
          accessTierShortName: AccessTierShortNames.Registered,
          eraRequired: false,
          eligible: false
        },{
          accessTierShortName: AccessTierShortNames.Controlled,
          eraRequired: true,
          eligible: false
        }]
      },
      load,
      reload,
      updateCache
    });
    wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(findIneligibleModule(wrapper, AccessModule.CTCOMPLIANCETRAINING).exists()).toBeTruthy();
  });

  it('Should display CT training when no institutional DUA', async() => {
    let wrapper = component();
    await waitOneTickAndUpdate(wrapper);

    profileStore.set({
      profile: {
        ...ProfileStubVariables.PROFILE_STUB,
        tierEligibilities: [{
          accessTierShortName: AccessTierShortNames.Registered,
          eraRequired: false,
          eligible: false
        }]
      },
      load,
      reload,
      updateCache
    });
    wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(findIneligibleModule(wrapper, AccessModule.CTCOMPLIANCETRAINING).exists()).toBeTruthy();
  });

  it('Should display CT training when eligible', async() => {
    let wrapper = component();
    await waitOneTickAndUpdate(wrapper);

    profileStore.set({
      profile: {
        ...ProfileStubVariables.PROFILE_STUB,
        tierEligibilities: [{
          accessTierShortName: AccessTierShortNames.Registered,
          eraRequired: false,
          eligible: false
        },{
          accessTierShortName: AccessTierShortNames.Controlled,
          eraRequired: true,
          eligible: true
        }]
      },
      load,
      reload,
      updateCache
    });
    wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(findIncompleteModule(wrapper, AccessModule.CTCOMPLIANCETRAINING).exists()).toBeTruthy();
  });

  it('Should allow CT and DUCC as simultaneously clickable', async() => {
    let wrapper = component();
    await waitOneTickAndUpdate(wrapper);

    profileStore.set({
      profile: {
        ...ProfileStubVariables.PROFILE_STUB,
        accessModules: {
          modules: allModules.map(moduleName => {
            if ([
              AccessModule.CTCOMPLIANCETRAINING,
              AccessModule.DATAUSERCODEOFCONDUCT
            ].includes(moduleName)) {
              return {moduleName};
            }
            return {moduleName, completionEpochMillis: 1};
          })
        },
        tierEligibilities: [{
          accessTierShortName: AccessTierShortNames.Registered,
          eraRequired: false,
          eligible: false
        },{
          accessTierShortName: AccessTierShortNames.Controlled,
          eraRequired: true,
          eligible: true
        }]
      },
      load,
      reload,
      updateCache});
    wrapper = component();
    await waitOneTickAndUpdate(wrapper);

    // Both are clickable.
    expect(findClickableModuleText(wrapper, AccessModule.CTCOMPLIANCETRAINING).exists()).toBeTruthy();
    expect(findClickableModuleText(wrapper, AccessModule.DATAUSERCODEOFCONDUCT).exists()).toBeTruthy();

    // Only the first module is active.
    expect(findNextCtaForModule(wrapper, AccessModule.CTCOMPLIANCETRAINING).exists()).toBeTruthy();
    expect(findNextCtaForModule(wrapper, AccessModule.DATAUSERCODEOFCONDUCT).exists()).toBeFalsy();
  });
});
