import * as React from "react";
import {mount} from "enzyme";

import defaultServerConfig from 'testing/default-server-config';
import {AccessModule, InstitutionApi, Profile, ProfileApi} from 'generated/fetch';
import {DataAccessRequirements} from './data-access-requirements';
import {InstitutionApiStub} from 'testing/stubs/institution-api-stub';
import {ProfileApiStub, ProfileStubVariables} from 'testing/stubs/profile-api-stub';
import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {profileStore, serverConfigStore} from 'app/utils/stores';

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
            profile,
            load,
            reload,
            updateCache,
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
