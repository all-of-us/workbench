import * as React from "react";

import {mount} from "enzyme";
import {DataAccessRequirements} from './data-access-requirements';
import {ProfileApiStub, ProfileStubVariables} from 'testing/stubs/profile-api-stub';
import {AccessModule, InstitutionApi, Profile, ProfileApi} from 'generated/fetch';
import {profileStore, serverConfigStore} from 'app/utils/stores';
import defaultServerConfig from "../../../testing/default-server-config";
import {profileApi, registerApiClient} from "../../services/swagger-fetch-clients";
import {InstitutionApiStub} from "../../../testing/stubs/institution-api-stub";

const load = jest.fn();
const reload = jest.fn();
const updateCache = jest.fn();
const profile: Profile = {
    ...ProfileStubVariables.PROFILE_STUB,
    accessModules: {
        modules: [
            {moduleName: AccessModule.TWOFACTORAUTH},
            {moduleName: AccessModule.RASLINKLOGINGOV},
            {moduleName: AccessModule.ERACOMMONS},
            {moduleName: AccessModule.COMPLIANCETRAINING}]
    }
};

describe('DataAccessRequirements', () => {
    const component = () => {
        return mount(<DataAccessRequirements hideSpinner={() => {}}/>);
    };

    beforeEach(async() => {
        registerApiClient(InstitutionApi, new InstitutionApiStub());
        registerApiClient(ProfileApi, new ProfileApiStub());

        serverConfigStore.set({config: defaultServerConfig});
        profileStore.set({
            profile: await profileApi().getMe(),
            load: jest.fn(),
            reload: jest.fn(),
            updateCache: jest.fn()
        });
    });

    it('should render', () => {
        profileStore.set({profile, load, reload, updateCache});
        const wrapper = component();
        expect(wrapper.exists()).toBeTruthy();
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
