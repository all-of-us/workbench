import * as React from 'react';
import {mount, ReactWrapper} from 'enzyme';

import {QuickTourAndVideos} from "./quick-tour-and-videos";
import {waitOneTickAndUpdate} from 'testing/react-test-helpers';

describe('Quick Tour and Videos', () => {
    const component = (initialQuickTour: boolean) => {
        return mount(<QuickTourAndVideos showQuickTourInitially={initialQuickTour}/>);
    };

    const getResource = (wrapper: ReactWrapper, index: number): ReactWrapper => {
        return wrapper.find(`[data-test-id="quick-tour-resource-${index}"]`)
    }

    const assertResourcesVisible = (wrapper: ReactWrapper, ...indices: number[]) => {
        indices.forEach(index => {
            expect(getResource(wrapper, index).exists()).toBeTruthy();
        });
    }

    const assertResourcesInvisible = (wrapper: ReactWrapper, ...indices: number[]) => {
        indices.forEach(index => {
             expect(getResource(wrapper, index).exists()).toBeFalsy();
        });
    }

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
        getResource(wrapper, 0).first().simulate('click');
        expect(wrapper.find('[data-test-id="quick-tour-react"]').exists()).toBeTruthy();
     });

    it('should scroll right and left', async () => {
        const wrapper = component(false);

        // there are 6 elements total, of which the first 4 are initially visible
        assertResourcesVisible(wrapper, 0, 1, 2, 3);
        assertResourcesInvisible(wrapper, 4, 5);

        // there is no left button, because we are viewing the leftmost resources
        expect(wrapper.find('[data-test-id="scroll-left"]').exists()).toBeFalsy();

        // but there is a right button which we can click
        expect(wrapper.find('[data-test-id="scroll-right"]').exists()).toBeTruthy();
        wrapper.find('[data-test-id="scroll-right"]').first().simulate('click');
        await waitOneTickAndUpdate(wrapper);

        assertResourcesInvisible(wrapper, 0);
        assertResourcesVisible(wrapper, 1, 2, 3, 4);
        assertResourcesInvisible(wrapper, 5);

        // both buttons are now available
        expect(wrapper.find('[data-test-id="scroll-left"]').exists()).toBeTruthy();
        expect(wrapper.find('[data-test-id="scroll-right"]').exists()).toBeTruthy();

        wrapper.find('[data-test-id="scroll-right"]').first().simulate('click');
        await waitOneTickAndUpdate(wrapper);
        assertResourcesInvisible(wrapper, 0, 1);
        assertResourcesVisible(wrapper, 2, 3, 4, 5);

        // we can no longer scroll right
        expect(wrapper.find('[data-test-id="scroll-right"]').exists()).toBeFalsy();
        expect(wrapper.find('[data-test-id="scroll-left"]').exists()).toBeTruthy();

        // now we scroll left
        wrapper.find('[data-test-id="scroll-left"]').first().simulate('click');
        await waitOneTickAndUpdate(wrapper);
        assertResourcesInvisible(wrapper, 0);
        assertResourcesVisible(wrapper, 1, 2, 3, 4);
        assertResourcesInvisible(wrapper, 5);
        expect(wrapper.find('[data-test-id="scroll-left"]').exists()).toBeTruthy();
        expect(wrapper.find('[data-test-id="scroll-right"]').exists()).toBeTruthy();

        wrapper.find('[data-test-id="scroll-left"]').first().simulate('click');
        await waitOneTickAndUpdate(wrapper);
        assertResourcesVisible(wrapper, 0, 1, 2, 3);
        assertResourcesInvisible(wrapper, 4, 5);

        // we can no longer scroll left
        expect(wrapper.find('[data-test-id="scroll-left"]').exists()).toBeFalsy();
        expect(wrapper.find('[data-test-id="scroll-right"]').exists()).toBeTruthy();
    });

});
