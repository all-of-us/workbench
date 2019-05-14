import {mount} from 'enzyme';
import * as React from 'react';

import {ResourceCard} from './component';
import {ConceptSetsApiStub} from 'testing/stubs/concept-sets-api-stub';
import {CohortsServiceStub} from 'testing/stubs/cohort-service-stub';
import { WorkspaceAccessLevel } from 'generated';

const ResourceCardWrapper = {
  cohortCard: {
    workspaceId: 1,
    workspaceNamespace: 'defaultNamespace',
    workspaceFirecloudName: 'defaultFirecloudName',
    permission: 'OWNER',
    cohort: new CohortsServiceStub().cohorts[0],
    modifiedTime: Date.now().toString()
  },
  conceptSetCard: {
    workspaceId: 1,
    workspaceNamespace: 'defaultNamespace',
    workspaceFirecloudName: 'defaultFirecloudName',
    permission: 'OWNER',
    conceptSet: new ConceptSetsApiStub().conceptSets[0],
    modifiedTime: Date.now().toString()
  },
  notebookCard: {
    workspaceId: 1,
    workspaceNamespace: 'defaultNamespace',
    workspaceFirecloudName: 'defaultFirecloudName',
    permission: 'OWNER',
    notebook: {
      'name': 'mockFile.ipynb',
      'path': 'gs://bucket/notebooks/mockFile.ipynb',
      'lastModifiedTime': 100
    },
    modifiedTime: Date.now().toString()
  }
};

describe('ResourceCardComponent', () => {
  const component = (resourceCard: Object) => {
    return mount(<ResourceCard
        resourceCard={resourceCard}
        onUpdate={() => {}}/>);
  };

  it('should render', () => {
    const wrapper = component(ResourceCardWrapper.cohortCard);
    expect(wrapper).toBeTruthy();
  });

  it('should render a cohort if resource is cohort', () => {
    const wrapper = component(ResourceCardWrapper.cohortCard);
    expect(wrapper.find('[data-test-id="card-type"]').text()).toBe('Cohort');
  });

  it('should render a concept set if resource is concept set', () => {
    const wrapper = component(ResourceCardWrapper.conceptSetCard);
    expect(wrapper.find('[data-test-id="card-type"]').text()).toBe('Concept Set');
  });

  it('should render a notebook if the resource is a notebook', () => {
    const wrapper = component(ResourceCardWrapper.notebookCard);
    expect(wrapper.find('[data-test-id="card-type"]').text()).toBe('Notebook');
  });

  // Note: this spec is not testing the Popup menus on resource cards due to an issue using
  //    PopupTrigger in the test suite.
});
