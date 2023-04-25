import * as React from 'react';
import { act } from 'react-dom/test-utils';
import { MemoryRouter } from 'react-router';
import { mount } from 'enzyme';
import { Dropdown } from 'primereact/dropdown';

import { NotebooksApi, WorkspaceAccessLevel } from 'generated/fetch';

import { AppFilesList } from 'app/pages/appAnalysis/app-files-list';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
import { APP_LIST, JUPYTER_APP } from 'app/utils/constants';
import { currentWorkspaceStore } from 'app/utils/navigation';

import {
  simulateComponentChange,
  waitOneTickAndUpdate,
} from 'testing/react-test-helpers';
import { NotebooksApiStub } from 'testing/stubs/notebooks-api-stub';
import { workspaceDataStub } from 'testing/stubs/workspaces';

describe('App Files list', () => {
  const startButton = (wrapper) => {
    return wrapper.find('[data-test-id="start-button"]').first();
  };

  const applicationListDropDownWrapper = (wrapper) => {
    return wrapper.find('[data-test-id="application-list-dropdown"]').first();
  };

  const nextButton = (wrapper) => {
    return wrapper.find('[data-test-id="next-btn"]').first();
  };

  const setCurrentWorkspaceAccessLevel = (accessLevel) => {
    currentWorkspaceStore.next({
      ...workspaceDataStub,
      accessLevel: accessLevel,
    });
  };

  const component = () => {
    return mount(
      <MemoryRouter>
        <AppFilesList hideSpinner={() => {}} />
        );
      </MemoryRouter>
    );
  };

  beforeEach(() => {
    currentWorkspaceStore.next({
      ...workspaceDataStub,
      cdrVersionId: '1',
    });
    registerApiClient(NotebooksApi, new NotebooksApiStub());
  });

  it('should render', () => {
    const wrapper = component();
    expect(wrapper.exists()).toBeTruthy();
  });

  it('should enable START button only if user has WRITER or OWNER ACCESS', () => {
    let wrapper = component();
    expect(wrapper.exists()).toBeTruthy();

    // By default the workspace access level is OWNER
    expect(startButton(wrapper).prop('disabled')).toBeFalsy();

    // Setting workspace Access level as READER
    setCurrentWorkspaceAccessLevel(WorkspaceAccessLevel.READER);
    wrapper = component();
    expect(startButton(wrapper).prop('disabled')).toBeTruthy();

    // Setting workspace Access level as WRITER
    setCurrentWorkspaceAccessLevel(WorkspaceAccessLevel.WRITER);
    wrapper = component();
    expect(startButton(wrapper).prop('disabled')).toBeFalsy();
  });

  it('should open select application modal that has list of applications on clicking start button', () => {
    const wrapper = component();
    startButton(wrapper).simulate('click');
    expect(
      wrapper.find('[data-test-id="select-application-modal"]')
    ).toBeTruthy();
    expect(applicationListDropDownWrapper(wrapper).prop('options')).toBe(
      APP_LIST
    );
  });

  it('should open jupyter modal when Jupyter application is selected and next button is clicked', async () => {
    const wrapper = component();
    startButton(wrapper).simulate('click');

    // Application Drop down List should have Jupyter as an option
    expect(applicationListDropDownWrapper(wrapper).prop('options')).toContain(
      JUPYTER_APP
    );

    // Next button should be disabled by default
    expect(nextButton(wrapper).prop('disabled')).toBe(true);

    await act(async () => {
      const applicationListDropDown = applicationListDropDownWrapper(
        wrapper
      ).instance() as Dropdown;
      await simulateComponentChange(
        wrapper,
        applicationListDropDown,
        JUPYTER_APP
      );
    });

    // Selecting an application from drop down should enable the NEXT button
    expect(nextButton(wrapper).prop('disabled')).toBe(false);
    expect(
      wrapper.find('[data-test-id="select-application-modal"]').length
    ).not.toBe(0);
    expect(wrapper.find('[data-test-id="jupyter-modal"]').length).toBe(0);

    // Clicking next button should stop showing the select application modal and show jupyter modal
    nextButton(wrapper).simulate('click');
    await waitOneTickAndUpdate(wrapper);
    expect(
      wrapper.find('[data-test-id="select-application-modal"]').length
    ).toBe(0);
    expect(wrapper.find('[data-test-id="jupyter-modal"]').length).not.toBe(0);
  });
});
