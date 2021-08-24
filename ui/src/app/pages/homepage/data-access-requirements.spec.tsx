import * as React from "react";

import {mount} from "enzyme";
import {DataAccessRequirements} from './data-access-requirements';
import {ProfileStubVariables} from 'testing/stubs/profile-api-stub';
import {AccessModule, Profile} from 'generated/fetch';
import {profileStore} from 'app/utils/stores';

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

    it('should render', () => {
        profileStore.set({profile, load, reload, updateCache});
        const wrapper = component();
        expect(wrapper.exists()).toBeTruthy();
    });
});
