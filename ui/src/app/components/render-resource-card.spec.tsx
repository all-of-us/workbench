import * as React from "react";

import {WorkspaceResource} from 'generated/fetch';
import {exampleCohortStubs} from 'testing/stubs/cohorts-api-stub';
import {renderResourceCard} from "./render-resource-card";
import {mount} from "enzyme";
import {stubResource} from 'testing/stubs/resources-stub';

describe('renderResourceCard', () => {
    it('renders a CohortResourceCard', () => {
         const testCohort = {
            ...stubResource,
            cohort: exampleCohortStubs[0],
        } as WorkspaceResource;

        const card = renderResourceCard({
            resource: testCohort,
            existingNameList: [],
            onUpdate: async () => {},
            menuOnly: false,
        });
        const wrapper = mount(card);
        expect(wrapper.exists()).toBeTruthy();
        expect(wrapper.text()).toContain('Cohort');
        expect(wrapper.text()).toContain(testCohort.cohort.name);
        expect(wrapper.text()).toContain(testCohort.cohort.description);
        expect(wrapper.text()).toContain('Last Modified');
    });

    it('does not render a card for an invalid resource', () => {
        const card = renderResourceCard({
            resource: stubResource,
            existingNameList: [],
            onUpdate: async () => {},
            menuOnly: false,
        });
        expect(card).toBeFalsy();
    })

    it('renders a Cohort menu without other card elements', () => {
        const testCohort = {
            ...stubResource,
            cohort: exampleCohortStubs[0],
        } as WorkspaceResource;

        const menu = renderResourceCard({
            resource: testCohort,
            existingNameList: [],
            onUpdate: async () => {},
            menuOnly: true,
        });
        const wrapper = mount(menu);
        expect(wrapper.exists()).toBeTruthy();
        expect(wrapper.text()).toBe('');
    });
})
