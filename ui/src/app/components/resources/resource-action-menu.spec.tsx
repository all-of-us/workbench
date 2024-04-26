import '@testing-library/jest-dom';

import * as React from 'react';
import { MemoryRouter } from 'react-router';
import { mount } from 'enzyme';

import { PrePackagedConceptSetEnum, WorkspaceResource } from 'generated/fetch';

import { serverConfigStore } from 'app/utils/stores';

import { exampleCohortStubs } from 'testing/stubs/cohorts-api-stub';
import { stubDataSet } from 'testing/stubs/data-set-api-stub';
import { stubResource } from 'testing/stubs/resources-stub';
import { workspaceDataStub } from 'testing/stubs/workspaces';

import { ResourceActionMenu } from './resource-action-menu';

describe(ResourceActionMenu.name, () => {
  const component = (card) => {
    return mount(<MemoryRouter>{card}</MemoryRouter>);
  };

  beforeEach(() => {
    serverConfigStore.set({
      config: { gsuiteDomain: '' },
    });
  });

  it('does not render a menu for an invalid resource', () => {
    // stubResource is only a base type for valid resources.
    // To be valid, it needs exactly one of these to be defined:
    // cohort, cohortReview, conceptSet, dataSet, notebook
    const invalidResource: WorkspaceResource = stubResource;
    const menu = (
      <ResourceActionMenu
        resource={invalidResource}
        workspace={workspaceDataStub}
        existingNameList={[]}
        onUpdate={async () => {}}
      />
    );
    expect(menu).not.toBeInTheDocument();
  });

  it('renders a Cohort menu', () => {
    const testCohort = {
      ...stubResource,
      cohort: exampleCohortStubs[0],
    } as WorkspaceResource;

    const menu = (
      <ResourceActionMenu
        resource={testCohort}
        workspace={workspaceDataStub}
        existingNameList={[]}
        onUpdate={async () => {}}
      />
    );
    const wrapper = component(menu);
    expect(wrapper.exists()).toBeTruthy();
    expect(wrapper.text()).toBe('');
  });

  it('renders a dataset menu', () => {
    const testDataSet = {
      ...stubResource,
      dataSet: {
        ...stubDataSet(),
        prePackagedConceptSet: [PrePackagedConceptSetEnum.FITBIT],
      },
    } as WorkspaceResource;

    const menu = (
      <ResourceActionMenu
        resource={testDataSet}
        workspace={workspaceDataStub}
        existingNameList={[]}
        onUpdate={async () => {}}
      />
    );
    const wrapper = component(menu);
    wrapper
      .find({ 'data-test-id': 'resource-card-menu' })
      .first()
      .simulate('click');
    expect(wrapper.text()).toContain('Export to Notebook');
    expect(wrapper.text()).not.toContain('Extract VCF Files');
  });

  it('renders a dataset menu, with WGS', () => {
    const testDataSet = {
      ...stubResource,
      dataSet: {
        ...stubDataSet(),
        prePackagedConceptSet: [
          PrePackagedConceptSetEnum.PERSON,
          PrePackagedConceptSetEnum.WHOLE_GENOME,
        ],
      },
    } as WorkspaceResource;

    const menu = (
      <ResourceActionMenu
        resource={testDataSet}
        workspace={workspaceDataStub}
        existingNameList={[]}
        onUpdate={async () => {}}
      />
    );
    const wrapper = component(menu);
    wrapper
      .find({ 'data-test-id': 'resource-card-menu' })
      .first()
      .simulate('click');
    expect(wrapper.text()).toContain('Export to Notebook');
    expect(wrapper.text()).toContain('Extract VCF Files');
  });
});
