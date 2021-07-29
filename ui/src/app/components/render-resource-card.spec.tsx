import * as React from 'react';

import {PrePackagedConceptSetEnum, WorkspaceResource} from 'generated/fetch';
import {MemoryRouter} from 'react-router';
import {exampleCohortStubs} from 'testing/stubs/cohorts-api-stub';
import {stubDataSet} from 'testing/stubs/data-set-api-stub';
import {renderResourceCard} from './render-resource-card';
import {mount} from 'enzyme';
import {stubResource} from 'testing/stubs/resources-stub';
import {serverConfigStore} from 'app/utils/stores';

describe('renderResourceCard', () => {

  const component = (card) => {
    return mount(<MemoryRouter>
      {card}
    </MemoryRouter>);
  };

  beforeEach(() => {
    serverConfigStore.set({config: {enableGenomicExtraction: true, gsuiteDomain: ''}});
  });

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
    const wrapper = component(card);
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
    const wrapper = component(menu);
    expect(wrapper.exists()).toBeTruthy();
    expect(wrapper.text()).toBe('');
  });

  it('renders a dataset menu', () => {
    const testDataSet = {
      ...stubResource,
      dataSet: {
        ...stubDataSet(),
        prePackagedConceptSet: [PrePackagedConceptSetEnum.FITBIT]
      }
    } as WorkspaceResource;

    const menu = renderResourceCard({
      resource: testDataSet,
      existingNameList: [],
      onUpdate: async () => {},
      menuOnly: true,
    });
    const wrapper = component(menu);
    wrapper.find({'data-test-id': 'resource-card-menu'}).first().simulate('click');
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
          PrePackagedConceptSetEnum.WHOLEGENOME
        ]
      },
    } as WorkspaceResource;

    const menu = renderResourceCard({
      resource: testDataSet,
      existingNameList: [],
      onUpdate: async () => {},
      menuOnly: true,
    });
    const wrapper = component(menu);
    wrapper.find({'data-test-id': 'resource-card-menu'}).first().simulate('click');
    expect(wrapper.text()).toContain('Export to Notebook');
    expect(wrapper.text()).toContain('Extract VCF Files');
  });
})
