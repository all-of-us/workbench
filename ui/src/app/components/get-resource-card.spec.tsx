import * as React from "react";
import {WorkspaceResource} from 'generated/fetch';
import {exampleCohortStubs} from 'testing/stubs/cohorts-api-stub';
import {getResourceCard} from "./get-resource-card";
import {mount} from "enzyme";
import {stubResource} from 'testing/stubs/resources-stub';

describe('getResourceCard', () => {
    it('renders a CohortResourceCard', () => {
         const testCohort = {
            ...stubResource,
            cohort: exampleCohortStubs[0],
        } as WorkspaceResource;

        const card = getResourceCard({
            resource: testCohort,
            existingNameList: [],
            onUpdate: async () => {},
        });
        const wrapper = mount(card);
        expect(wrapper.exists()).toBeTruthy();
    });

    it('does not render a card for an invalid resource', () => {
        const card = getResourceCard({
            resource: stubResource,
            existingNameList: [],
            onUpdate: async () => {},
        });
        expect(card).toBeFalsy();
    })
})
