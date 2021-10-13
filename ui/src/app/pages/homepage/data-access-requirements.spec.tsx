import * as React from "react";
import {mount} from "enzyme";

import defaultServerConfig from 'testing/default-server-config';
import {AccessModule, InstitutionApi, Profile, ProfileApi} from 'generated/fetch';
import {allModules, DataAccessRequirements, getActiveModule, getEnabledModules} from './data-access-requirements';
import {InstitutionApiStub} from 'testing/stubs/institution-api-stub';
import {ProfileApiStub, ProfileStubVariables} from 'testing/stubs/profile-api-stub';
import {profileApi, registerApiClient} from 'app/services/swagger-fetch-clients';
import {profileStore, serverConfigStore} from 'app/utils/stores';
import {MemoryRouter} from 'react-router-dom';
import {waitForFakeTimersAndUpdate, waitOneTickAndUpdate} from 'testing/react-test-helpers';


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

    const findCompletionBanner = (wrapper) => wrapper.find('[data-test-id="dar-completed"]');

    beforeEach(async() => {
        registerApiClient(InstitutionApi, new InstitutionApiStub());
        registerApiClient(ProfileApi, new ProfileApiStub());

        serverConfigStore.set({config: defaultServerConfig});
        profileStore.set({profile, load, reload, updateCache});
    });

    afterEach(() => {
        // reset to standard behavior after tests which use fake timers
        jest.useRealTimers();
    })

    it('should return all modules from getEnabledModules by default (all FFs enabled)', () => {
        const enabledModules = getEnabledModules(allModules);
        allModules.forEach(module => expect(enabledModules.includes(module)).toBeTruthy());
    });

    it('should not return the RAS module from getEnabledModules when its feature flag is disabled', () => {
        serverConfigStore.set({config: {...defaultServerConfig, enableRasLoginGovLinking: false, enforceRasLoginGovLinking: false}});
        const enabledModules = getEnabledModules(allModules);
        expect(enabledModules.includes(AccessModule.RASLINKLOGINGOV)).toBeFalsy();
    });

    it('should return the RAS module from getEnabledModules when enforceRasLoginGovLinking is enabled, enableRasLoginGovLinking is not', () => {
        serverConfigStore.set({config: {...defaultServerConfig, enableRasLoginGovLinking: false, enforceRasLoginGovLinking: true}});
        const enabledModules = getEnabledModules(allModules);
        expect(enabledModules.includes(AccessModule.RASLINKLOGINGOV)).toBeTruthy();
    });

    it('should not return the ERA module from getEnabledModules when its feature flag is disabled', () => {
        serverConfigStore.set({config: {...defaultServerConfig, enableEraCommons: false}});
        const enabledModules = getEnabledModules(allModules);
        expect(enabledModules.includes(AccessModule.ERACOMMONS)).toBeFalsy();
    });

    it('should not return the Compliance module from getEnabledModules when its feature flag is disabled', () => {
        serverConfigStore.set({config: {...defaultServerConfig, enableComplianceTraining: false}});
        const enabledModules = getEnabledModules(allModules);
        expect(enabledModules.includes(AccessModule.COMPLIANCETRAINING)).toBeFalsy();
    });

    it('should return the first module (2FA) from getActiveModule when no modules have been completed', () => {
        const enabledModules = getEnabledModules(allModules);
        const activeModule = getActiveModule(enabledModules, profile);

        expect(activeModule).toEqual(allModules[0]);
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

        const enabledModules = getEnabledModules(allModules);
        const activeModule = getActiveModule(enabledModules, testProfile);

        expect(activeModule).toEqual(allModules[1]);
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

        const enabledModules = getEnabledModules(allModules);
        const activeModule = getActiveModule(enabledModules, testProfile);

        expect(activeModule).toEqual(allModules[1]);
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

        const enabledModules = getEnabledModules(allModules);
        const activeModule = getActiveModule(enabledModules, testProfile);

        // update this if the order changes
        expect(activeModule).toEqual(AccessModule.ERACOMMONS)

        // 2FA (module 0) is complete, so enabled #1 is active
        expect(activeModule).toEqual(enabledModules[1]);

        // but we skip allModules[1] because it's RAS and is not enabled
        expect(activeModule).toEqual(allModules[2]);
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

        const enabledModules = getEnabledModules(allModules);
        const activeModule = getActiveModule(enabledModules, testProfile);

        expect(activeModule).toEqual(allModules[3]);
        expect(activeModule).toEqual(enabledModules[3]);

        // update this if the order changes
        expect(activeModule).toEqual(AccessModule.COMPLIANCETRAINING)
    });

    it('should return undefined from getActiveModule when all modules have been completed', () => {
        const testProfile = {
            ...profile,
            accessModules: {
                modules: allModules.map(module => ({moduleName: module, completionEpochMillis: 1}))
            }
        };

        const enabledModules = getEnabledModules(allModules);
        const activeModule = getActiveModule(enabledModules, testProfile);

        expect(activeModule).toBeUndefined();
    });

    it('should not indicate the RAS module as active when a user has completed it', () => {
        // initially, the user has completed all modules except RAS (the standard case at RAS launch time)
        const testProfile = {
            ...profile,
            accessModules: {
                modules: [
                    {moduleName: AccessModule.TWOFACTORAUTH, completionEpochMillis: 1},
                    {moduleName: AccessModule.ERACOMMONS, completionEpochMillis: 1},
                    {moduleName: AccessModule.COMPLIANCETRAINING, completionEpochMillis: 1},
                    {moduleName: AccessModule.DATAUSERCODEOFCONDUCT, completionEpochMillis: 1},
                ]
            }
        };

        const enabledModules = getEnabledModules(allModules);

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

    it('should render all modules by default (all FFs enabled)', () => {
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

    it('should render all modules as incomplete when the profile accessModules are empty', () => {
        const wrapper = component();
        allModules.forEach(module => {
            expect(findIncompleteModule(wrapper, module).exists()).toBeTruthy();

            expect(findCompleteModule(wrapper, module).exists()).toBeFalsy();
            expect(findIneligibleModule(wrapper, module).exists()).toBeFalsy();
        });
        expect(findCompletionBanner(wrapper).exists()).toBeFalsy();
    });

    it('should render all modules as complete when the profile accessModules are all complete', () => {
        profileStore.set({
            profile: {
                ...ProfileStubVariables.PROFILE_STUB,
                accessModules: {
                    modules: allModules.map(module => ({moduleName: module, completionEpochMillis: 1}))
                }
            },
            load,
            reload,
            updateCache});

        const wrapper = component();
        allModules.forEach(module => {
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

        // initially, the user has completed all modules except RAS (the standard case at RAS launch time)

        const allExceptRas = allModules.filter(m => m !== AccessModule.RASLINKLOGINGOV);
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

        // now all modules are complete

        profileStore.set({
            profile: {
                ...ProfileStubVariables.PROFILE_STUB,
                accessModules: {
                    modules: allModules.map(module => ({moduleName: module, completionEpochMillis: 1}))
                }
            },
            load,
            reload,
            updateCache});

        await waitForFakeTimersAndUpdate(wrapper);

        allModules.forEach(module => {
            expect(findCompleteModule(wrapper, module).exists()).toBeTruthy();

            expect(findIncompleteModule(wrapper, module).exists()).toBeFalsy();
            expect(findIneligibleModule(wrapper, module).exists()).toBeFalsy();
        });

        expect(findCompletionBanner(wrapper).exists()).toBeTruthy();
    });


    it('should render all modules as complete when the profile accessModules are all bypassed', () => {
        profileStore.set({
            profile: {
                ...ProfileStubVariables.PROFILE_STUB,
                accessModules: {
                    modules: allModules.map(module => {return {moduleName: module, bypassEpochMillis: 1}})
                }
            },
            load,
            reload,
            updateCache});

        const wrapper = component();
        allModules.forEach(module => {
            expect(findCompleteModule(wrapper, module).exists()).toBeTruthy();

            expect(findIncompleteModule(wrapper, module).exists()).toBeFalsy();
            expect(findIneligibleModule(wrapper, module).exists()).toBeFalsy();
        });
        expect(findCompletionBanner(wrapper).exists()).toBeTruthy();
    });

    it('should render a mix of complete and incomplete modules, as appropriate', () => {
        const incompleteModules = [AccessModule.RASLINKLOGINGOV];
        const completeModules = allModules.filter(module => module !== AccessModule.RASLINKLOGINGOV);

        // sanity check
        expect(incompleteModules.length).toEqual(1);
        expect(completeModules.length).toEqual(4);

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

    it('should not show self-bypass UI when all modules are complete', () => {
        serverConfigStore.set({config: {...serverConfigStore.get().config, unsafeAllowSelfBypass: true}});
        profileStore.set({
            profile: {
                ...ProfileStubVariables.PROFILE_STUB,
                accessModules: {
                    modules: allModules.map(module => {return {moduleName: module, completionEpochMillis: 1}})
                }
            },
            load,
            reload,
            updateCache});

        const wrapper = component();
        expect(wrapper.find('[data-test-id="self-bypass"]').exists()).toBeFalsy();
    });

    // regression tests for RW-7384: sync external modules to gain access

    it('should sync incomplete external modules', async() => {
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
                    modules: allModules.map(module => ({moduleName: module, completionEpochMillis: 1}))
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

});
