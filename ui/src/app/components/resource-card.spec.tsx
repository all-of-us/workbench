import {mount, ReactWrapper} from 'enzyme';
import * as React from 'react';

import { WorkspaceAccessLevel } from 'generated';
import {ResourceCard} from './resource-card';
import {CohortsApiStub} from 'testing/stubs/cohorts-api-stub';
import {ConceptSetsApiStub} from 'testing/stubs/concept-sets-api-stub';

const ResourceCardWrapper = {
  cohortCard: {
    workspaceId: 1,
    workspaceNamespace: 'defaultNamespace',
    workspaceFirecloudName: 'defaultFirecloudName',
    permission: 'OWNER',
    cohort: new CohortsApiStub().cohorts[0],
    modifiedTime: new Date().toISOString()
  },
  conceptSetCard: {
    workspaceId: 1,
    workspaceNamespace: 'defaultNamespace',
    workspaceFirecloudName: 'defaultFirecloudName',
    permission: 'OWNER',
    conceptSet: new ConceptSetsApiStub().conceptSets[0],
    modifiedTime: new Date().toISOString()
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
    modifiedTime: new Date().toISOString()
  }
};

describe('ResourceCardComponent', () => {
  const component = (resourceCard: Object) => {
    return mount(<ResourceCard
        onDuplicateResource={() => {}}
        resourceCard={resourceCard}
        onUpdate={() => {}}/>);
  };

  const lastModifiedDate = (wrapper: ReactWrapper) => {
    const txt = wrapper.find('[data-test-id="last-modified"]').text().replace('Last Modified: ', '');
    return Date.parse(txt);
  };

  it('should render', () => {
    const wrapper = component(ResourceCardWrapper.cohortCard);
    expect(wrapper).toBeTruthy();
  });

  it('should render a cohort if resource is cohort', () => {
    const wrapper = component(ResourceCardWrapper.cohortCard);
    expect(wrapper.find('[data-test-id="card-type"]').text()).toBe('Cohort');
    expect(lastModifiedDate(wrapper)).toBeTruthy();
  });

  it('should render a concept set if resource is concept set', () => {
    const wrapper = component(ResourceCardWrapper.conceptSetCard);
    expect(wrapper.find('[data-test-id="card-type"]').text()).toBe('Concept Set');
    expect(lastModifiedDate(wrapper)).toBeTruthy();
  });

  it('should render a notebook if the resource is a notebook', () => {
    const wrapper = component(ResourceCardWrapper.notebookCard);
    expect(wrapper.find('[data-test-id="card-type"]').text()).toBe('Notebook');
    expect(lastModifiedDate(wrapper)).toBeTruthy();
  });

  // Note: this spec is not testing the Popup menus on resource cards due to an issue using
  //    PopupTrigger in the test suite.
});
