import * as React from "react";

import {mount} from "enzyme";
import { DataAccessRequirements } from './data-access-requirements';

describe('DataAccessRequirements', () => {

    const component = () => { 
        return mount(<DataAccessRequirements hideSpinner={() => {}}/>);
    };

    it('should render', () => {
        const wrapper = component();
        expect(wrapper.exists()).toBeTruthy();
    });
});