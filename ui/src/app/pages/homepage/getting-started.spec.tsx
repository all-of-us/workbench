import * as React from 'react';
import {mount} from 'enzyme';

import { GettingStarted } from './getting-started';

describe('Getting Started', () => {
    const component = () => {
        return mount(<GettingStarted/>);
    };

    it('should render', () => {
        const wrapper = component();
        expect(wrapper.exists()).toBeTruthy();
    });
});
