import * as React from 'react';
import {mount} from 'enzyme';

import { WelcomeHeader } from './welcome-header';

describe('WelcomeHeader', () => {
    const component = () => {
        return mount(<WelcomeHeader/>);
    };

    it('should render', () => {
        const wrapper = component();
        expect(wrapper.exists()).toBeTruthy();
    });
});
