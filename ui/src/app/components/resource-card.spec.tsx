import {mount, ReactWrapper} from 'enzyme';
import * as React from 'react';

import {cohortReviewStubs} from 'testing/stubs/cohort-review-service-stub';
import {CohortsApiStub} from 'testing/stubs/cohorts-api-stub';
import {ConceptSetsApiStub} from 'testing/stubs/concept-sets-api-stub';
import {ResourceCard} from './resource-card';
import {BillingStatus,WorkspaceResource} from 'generated/fetch';
import {CdrVersionsStubVariables} from "../../testing/stubs/cdr-versions-api-stub";

const ResourceCardWrapper = {
  cohortCard: {
    workspaceId: 1,
    workspaceNamespace: 'defaultNamespace',
    workspaceFirecloudName: 'defaultFirecloudName',
    permission: 'OWNER',
    workspaceBillingStatus: BillingStatus.ACTIVE,
    cdrVersionId: CdrVersionsStubVariables.DEFAULT_WORKSPACE_CDR_VERSION_ID,
    cohort: new CohortsApiStub().cohorts[0],
    modifiedTime: new Date().toISOString()
  } as WorkspaceResource,
  readonlyCohortCard: {
    workspaceId: 1,
    workspaceNamespace: 'defaultNamespace',
    workspaceFirecloudName: 'defaultFirecloudName',
    permission: 'READER',
    workspaceBillingStatus: BillingStatus.ACTIVE,
    cdrVersionId: CdrVersionsStubVariables.DEFAULT_WORKSPACE_CDR_VERSION_ID,
    cohort: new CohortsApiStub().cohorts[0],
    modifiedTime: new Date().toISOString()
  } as WorkspaceResource,
  cohortReviewCard: {
    workspaceId: 1,
    workspaceNamespace: 'defaultNamespace',
    workspaceFirecloudName: 'defaultFirecloudName',
    permission: 'OWNER',
    workspaceBillingStatus: BillingStatus.ACTIVE,
    cdrVersionId: CdrVersionsStubVariables.DEFAULT_WORKSPACE_CDR_VERSION_ID,
    cohortReview: cohortReviewStubs[0],
    modifiedTime: new Date().toISOString()
  } as WorkspaceResource,
  readonlyCohortReviewCard: {
    workspaceId: 1,
    workspaceNamespace: 'defaultNamespace',
    workspaceFirecloudName: 'defaultFirecloudName',
    permission: 'READER',
    workspaceBillingStatus: BillingStatus.ACTIVE,
    cdrVersionId: CdrVersionsStubVariables.DEFAULT_WORKSPACE_CDR_VERSION_ID,
    cohortReview: cohortReviewStubs[0],
    modifiedTime: new Date().toISOString()
  } as WorkspaceResource,
  conceptSetCard: {
    workspaceId: 1,
    workspaceNamespace: 'defaultNamespace',
    workspaceFirecloudName: 'defaultFirecloudName',
    permission: 'OWNER',
    workspaceBillingStatus: BillingStatus.ACTIVE,
    cdrVersionId: CdrVersionsStubVariables.DEFAULT_WORKSPACE_CDR_VERSION_ID,
    conceptSet: new ConceptSetsApiStub().conceptSets[0],
    modifiedTime: new Date().toISOString()
  } as WorkspaceResource,
  readonlyConceptSetCard: {
    workspaceId: 1,
    workspaceNamespace: 'defaultNamespace',
    workspaceFirecloudName: 'defaultFire cloudName',
    permission: 'READER',
    workspaceBillingStatus: BillingStatus.ACTIVE,
    cdrVersionId: CdrVersionsStubVariables.DEFAULT_WORKSPACE_CDR_VERSION_ID,
    conceptSet: new ConceptSetsApiStub().conceptSets[0],
    modifiedTime: new Date().toISOString()
  } as WorkspaceResource,
  notebookCard: {
    workspaceId: 1,
    workspaceNamespace: 'defaultNamespace',
    workspaceFirecloudName: 'defaultFirecloudName',
    permission: 'OWNER',
    workspaceBillingStatus: BillingStatus.ACTIVE,
    cdrVersionId: CdrVersionsStubVariables.DEFAULT_WORKSPACE_CDR_VERSION_ID,
    notebook: {
      'name': 'mockFile.ipynb',
      'path': 'gs://bucket/notebooks/mockFile.ipynb',
      'lastModifiedTime': 100
    },
    modifiedTime: new Date().toISOString()
  } as WorkspaceResource,
  readonlyNotebookCard: {
    workspaceId: 1,
    workspaceNamespace: 'defaultNamespace',
    workspaceFirecloudName: 'defaultFirecloudName',
    permission: 'READER',
    workspaceBillingStatus: BillingStatus.ACTIVE,
    cdrVersionId: CdrVersionsStubVariables.DEFAULT_WORKSPACE_CDR_VERSION_ID,
    notebook: {
      'name': 'mockFile.ipynb',
      'path': 'gs://bucket/notebooks/mockFile.ipynb',
      'lastModifiedTime': 100
    },
    modifiedTime: new Date().toISOString()
  } as WorkspaceResource,
};

describe('ResourceCardComponent', () => {
  const component = (resourceCard: WorkspaceResource) => {
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

  it('should render a cohort review if resource is cohort review', () => {
    const wrapper = component(ResourceCardWrapper.cohortReviewCard);
    expect(wrapper.find('[data-test-id="card-type"]').text()).toBe('Cohort Review');
    expect(lastModifiedDate(wrapper)).toBeTruthy();
  });

  it('should render a concept set if resource is concept set', () => {
    const wrapper = component(ResourceCardWrapper.conceptSetCard);
    expect(wrapper.find('[data-test-id="card-type"]').text()).toBe('Concept Set');
    expect(lastModifiedDate(wrapper)).toBeTruthy();
  });

  function findResourceMenuProps(wrapper: ReactWrapper) {
    return wrapper.find('[data-test-id="resource-menu"]').props()
  }

  // We can't test the popup menus themselves in this test because PopupTrigger doesn't know how to draw the modal
  // without a browser to define bounding boxes...
  it('should always have a clickable popup menu (cohort)', () => {
    const wrapper = component(ResourceCardWrapper.readonlyCohortCard);
    expect(findResourceMenuProps(wrapper)).not.toContain('disabled');
  });

  it('should always have a clickable popup menu (cohort review)', () => {
    const wrapper = component(ResourceCardWrapper.readonlyCohortReviewCard);
    expect(findResourceMenuProps(wrapper)).not.toContain('disabled');
  });

  it('should always have a clickable popup menu (concept set)', () => {
    const wrapper = component(ResourceCardWrapper.readonlyConceptSetCard);
    expect(findResourceMenuProps(wrapper)).not.toContain('disabled');
  });

  it('should always have a clickable popup menu (notebook)', () => {
    const wrapper = component(ResourceCardWrapper.readonlyNotebookCard);
    expect(findResourceMenuProps(wrapper)).not.toContain('disabled');
  });
});
