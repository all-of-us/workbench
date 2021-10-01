import {mount} from 'enzyme';
import * as React from 'react';
import { MemoryRouter } from 'react-router-dom';

import {NotificationBanner} from "./notification-banner";

describe('NotificationBanner', () => {
    const component = () => {
        return mount(<MemoryRouter><NotificationBanner/></MemoryRouter>);
    };

    it('should render', () => {
        const wrapper = component();
        expect(wrapper.exists()).toBeTruthy();
    });
});
