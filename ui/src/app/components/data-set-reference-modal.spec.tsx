import {mount} from "enzyme";
import * as React from "react";
import {DatasetReferenceModal} from "./data-set-reference-modal";
import {WorkspaceResource} from 'generated/fetch';
import {exampleCohortStubs} from 'testing/stubs/cohorts-api-stub';
import {Button} from "./buttons";
import {datasetApi, registerApiClient} from 'app/services/swagger-fetch-clients';
import {waitOneTickAndUpdate} from 'testing/react-test-helpers';
import {DatasetApi} from 'generated/fetch';
import {DatasetApiStub} from 'testing/stubs/data-set-api-stub';
import {stubResource} from 'testing/stubs/resources-stub';

const testCohort = {
    ...stubResource,
    cohort: exampleCohortStubs[0],
} as WorkspaceResource;

const onCancelFn = jest.fn();
const deleteResourceFn = jest.fn();

const mountModal = () => {
    return mount(<DatasetReferenceModal
        referencedResource={testCohort}
        datasets=''
        onCancel={onCancelFn}
        deleteResource={deleteResourceFn}/>);
}

describe('DatasetReferenceModal', () => {
    it('should render', async () => {
        const wrapper = mountModal();
        expect(wrapper.exists()).toBeTruthy();
    });

    it('should cancel deletion', async () => {
        const wrapper = mountModal();

        const cancelButton = wrapper.find(Button).find({type: 'secondary'});
        cancelButton.simulate('click');

        expect(onCancelFn).toHaveBeenCalled();
        expect(deleteResourceFn).not.toHaveBeenCalled();
    });

    it('should markDirty and delete', async () => {
        registerApiClient(DatasetApi, new DatasetApiStub());
        const markDirty = jest.spyOn(datasetApi(), 'markDirty');
        const wrapper = mountModal();

        const deleteButton = wrapper.find(Button).find({type: 'primary'});
        deleteButton.simulate('click');
        await waitOneTickAndUpdate(wrapper);

        expect(markDirty).toHaveBeenCalled();
        expect(deleteResourceFn).toHaveBeenCalled();

        expect(onCancelFn).not.toHaveBeenCalled();
    });
});
