import * as React from 'react';
import {mount} from 'enzyme';

import {QuickTourAndVideos} from "./quick-tour-and-videos";

describe('Quick Tour and Videos', () => {
    const component = () => {
        return mount(<QuickTourAndVideos showQuickTourInitially={false}/>);
    };

    it('should render', () => {
        const wrapper = component();
        expect(wrapper.exists()).toBeTruthy();
    });
});
