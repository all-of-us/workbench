import {mount} from 'enzyme';
import * as React from 'react';

import {ProfileAccessModules} from "./profile-access-modules";

describe('Profile Access Modules', () => {
    const component = (props = {}) => {
        return mount(<ProfileAccessModules {...props}/>);
    };

    it('Should render', async () => {
        const wrapper = component();
        expect(wrapper.exists()).toBeTruthy();
    });
});
