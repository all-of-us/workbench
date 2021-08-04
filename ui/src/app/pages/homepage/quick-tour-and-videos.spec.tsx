import * as React from 'react';
import {mount} from 'enzyme';

import {QuickTourAndVideos} from "./quick-tour-and-videos";

describe('Quick Tour and Videos', () => {
    const component = (initialQuickTour: boolean) => {
        return mount(<QuickTourAndVideos showQuickTourInitially={initialQuickTour}/>);
    };

    it('should auto-display quick tour when requested', () => {
        const wrapper = component(true);
        expect(wrapper.find('[data-test-id="quick-tour-react"]').exists()).toBeTruthy();
    });

    it('should not auto-display quick tour when not requested', () => {
        const wrapper = component(false);
        expect(wrapper.find('[data-test-id="quick-tour-react"]').exists()).toBeFalsy();
    });

    it('should display quick tour when clicked', () => {
        const wrapper = component(false);
        wrapper.find('[data-test-id="quick-tour-resource-0"]').first().simulate('click');
        expect(wrapper.find('[data-test-id="quick-tour-react"]').exists()).toBeTruthy();
     });
});
