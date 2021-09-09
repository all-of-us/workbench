import {serverConfigStore} from 'app/utils/stores';
import {mount} from 'enzyme';
import {Profile} from 'generated/fetch';
import * as React from 'react';
import defaultServerConfig from 'testing/default-server-config';
import {ProfileStubVariables} from 'testing/stubs/profile-api-stub';

import {ProfileAccessModules} from './profile-access-modules';

const profile = ProfileStubVariables.PROFILE_STUB as Profile;

describe('Profile Access Modules', () => {
    beforeEach(async () => {
        serverConfigStore.set({config: defaultServerConfig});
    });

    const component = () => {
        return mount(<ProfileAccessModules profile={profile}/>);
    };

    it('Should render', async () => {
        const wrapper = component();
        expect(wrapper.exists()).toBeTruthy();
    });

    it('should display all modules as incomplete by default', async () => {
        const wrapper = component();
        const profileCardCompleteButtons = wrapper.find('[data-test-id="incomplete-button"]');
        expect(profileCardCompleteButtons.length).toBe(4);
    });
});
