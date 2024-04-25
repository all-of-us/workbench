import * as React from 'react';
import { MemoryRouter } from 'react-router';
import { mount } from 'enzyme';

import { PrePackagedConceptSetEnum, WorkspaceResource } from 'generated/fetch';

import { serverConfigStore } from 'app/utils/stores';

import { exampleCohortStubs } from 'testing/stubs/cohorts-api-stub';
import { stubDataSet } from 'testing/stubs/data-set-api-stub';
import { stubResource } from 'testing/stubs/resources-stub';
import { workspaceDataStub } from 'testing/stubs/workspaces';

import { renderResourceMenu } from './render-resource-menu';

describe(renderResourceMenu.name, () => {
  const component = (card) => {
    return mount(<MemoryRouter>{card}</MemoryRouter>);
  };

  beforeEach(() => {
    serverConfigStore.set({
      config: { gsuiteDomain: '' },
    });
  });

  it('does not render a menu for an invalid resource', () => {
    const menu = renderResourceMenu(
      stubResource,
      workspaceDataStub,
      [],
      async () => {}
    );
    expect(menu).toBeFalsy();
  });

  it('renders a Cohort menu', () => {
    const testCohort = {
      ...stubResource,
      cohort: exampleCohortStubs[0],
    } as WorkspaceResource;

    const menu = renderResourceMenu(
      testCohort,
      workspaceDataStub,
      [],
      async () => {}
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

    const menu = renderResourceMenu(
      testDataSet,
      workspaceDataStub,
      [],
      async () => {}
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

    const menu = renderResourceMenu(
      testDataSet,
      workspaceDataStub,
      [],
      async () => {}
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
