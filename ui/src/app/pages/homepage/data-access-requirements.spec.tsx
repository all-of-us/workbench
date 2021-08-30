import * as React from "react";
import {mount} from "enzyme";

import defaultServerConfig from 'testing/default-server-config';
import {AccessModule, InstitutionApi, Profile, ProfileApi} from 'generated/fetch';
import {allModules, DataAccessRequirements} from './data-access-requirements';
import {InstitutionApiStub} from 'testing/stubs/institution-api-stub';
import {ProfileApiStub, ProfileStubVariables} from 'testing/stubs/profile-api-stub';
import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {profileStore, serverConfigStore} from 'app/utils/stores';

const profile = ProfileStubVariables.PROFILE_STUB as Profile;
const load = jest.fn();
const reload = jest.fn();
const updateCache = jest.fn();

describe('DataAccessRequirements', () => {
    const component = () => {
        return mount(<DataAccessRequirements hideSpinner={() => {}}/>);
    };

    const findModule = (wrapper, module: AccessModule) => wrapper.find(`[data-test-id="module-${module}"]`);

    beforeEach(async() => {
        registerApiClient(InstitutionApi, new InstitutionApiStub());
        registerApiClient(ProfileApi, new ProfileApiStub());

        serverConfigStore.set({config: defaultServerConfig});
        profileStore.set({profile, load, reload, updateCache});
    });

    it('should render the expected modules', () => {
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

    it('should not show self-bypass UI when unsafeSelfBypass is false', () => {
        serverConfigStore.set({config: {...serverConfigStore.get().config, unsafeAllowSelfBypass: false}});
        const wrapper = component();
        expect(wrapper.find('[data-test-id="self-bypass"]').length).toBe(0);
    });

    it('should show self-bypass when unsafeSelfBypass is true', () => {
        serverConfigStore.set({config: {...serverConfigStore.get().config, unsafeAllowSelfBypass: true}});
        const wrapper = component();
        expect(wrapper.find('[data-test-id="self-bypass"]').length).toBe(2);
    });

});
