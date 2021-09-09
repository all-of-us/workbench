import * as React from "react";
import {mount} from "enzyme";

import defaultServerConfig from 'testing/default-server-config';
import {AccessModule, InstitutionApi, Profile, ProfileApi} from 'generated/fetch';
import {allModules, DataAccessRequirements} from './data-access-requirements';
import {InstitutionApiStub} from 'testing/stubs/institution-api-stub';
import {ProfileApiStub, ProfileStubVariables} from 'testing/stubs/profile-api-stub';
import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {profileStore, serverConfigStore} from 'app/utils/stores';
import { MemoryRouter } from 'react-router-dom';

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

    it('should render all modules by default', () => {
        const wrapper = component();
        allModules.forEach(module => expect(findModule(wrapper, module).exists()).toBeTruthy());
    });

    it('should not render the RAS module when its feature flag is disabled', () => {
        serverConfigStore.set({config: {...defaultServerConfig, enableRasLoginGovLinking: false}});
        const wrapper = component();
        expect(findModule(wrapper, AccessModule.RASLINKLOGINGOV).exists()).toBeFalsy();
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
                    modules: allModules.map(module => {return {moduleName: module, completionEpochMillis: 1}})
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

});
