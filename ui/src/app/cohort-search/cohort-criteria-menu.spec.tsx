import * as React from 'react';
import { mount } from 'enzyme';

import { CohortBuilderApi } from 'generated/fetch';

import {
  cohortBuilderApi,
  registerApiClient,
} from 'app/services/swagger-fetch-clients';
import { currentWorkspaceStore } from 'app/utils/navigation';

import { waitOneTickAndUpdate } from 'testing/react-test-helpers';
import { CohortBuilderServiceStub } from 'testing/stubs/cohort-builder-service-stub';
import { workspaceDataStub } from 'testing/stubs/workspaces';

import { CohortCriteriaMenu } from './cohort-criteria-menu';

describe('CohortCriteriaMenu', () => {
  const component = () => {
    return mount(
      <CohortCriteriaMenu launchSearch={() => {}} menuOptions={[]} />
    );
  };

  beforeEach(() => {
    registerApiClient(CohortBuilderApi, new CohortBuilderServiceStub());
    currentWorkspaceStore.next({
      ...workspaceDataStub,
      cdrVersionId: '1',
    });
  });

  it('should render', () => {
    const wrapper = component();
    expect(wrapper.exists()).toBeTruthy();
  });

  it('should open the menu on button click', () => {
    const wrapper = component();
    expect(
      wrapper.find('[data-test-id="criteria-menu-dropdown"]').exists()
    ).toBeFalsy();
    wrapper
      .find('[data-test-id="criteria-menu-button"]')
      .first()
      .simulate('click');
    expect(
      wrapper.find('[data-test-id="criteria-menu-dropdown"]').exists()
    ).toBeTruthy();
  });

  it('should call the api when a valid search term is entered', async () => {
    const wrapper = component();
    const shortInput = 'i';
    const validInput = 'valid input';
    wrapper
      .find('[data-test-id="criteria-menu-button"]')
      .first()
      .simulate('click');
    const domainCountsSpy = jest.spyOn(
      cohortBuilderApi(),
      'findUniversalDomainCounts'
    );
    expect(domainCountsSpy).toHaveBeenCalledTimes(0);

    // Show the alert message when only a single char is entered
    wrapper
      .find('[data-test-id="criteria-menu-input"]')
      .first()
      .simulate('change', { target: { value: shortInput } });
    await waitOneTickAndUpdate(wrapper);
    wrapper
      .find('[data-test-id="criteria-menu-input"]')
      .first()
      .simulate('keydown', { key: 'Enter' });
    await waitOneTickAndUpdate(wrapper);
    expect(
      wrapper.find('[data-test-id="criteria-menu-input"]').exists()
    ).toBeTruthy();
    expect(
      wrapper.find('[data-test-id="criteria-menu-input-alert"]').exists()
    ).toBeTruthy();
    expect(domainCountsSpy).toHaveBeenCalledTimes(0);

    // No alert and call api for valid search term
    wrapper
      .find('[data-test-id="criteria-menu-input"]')
      .first()
      .simulate('change', { target: { value: validInput } });
    wrapper
      .find('[data-test-id="criteria-menu-input"]')
      .first()
      .simulate('keydown', { key: 'Enter' });
    await waitOneTickAndUpdate(wrapper);
    expect(
      wrapper.find('[data-test-id="criteria-menu-input-alert"]').exists()
    ).toBeFalsy();
    expect(domainCountsSpy).toHaveBeenCalledTimes(1);
  });
});
