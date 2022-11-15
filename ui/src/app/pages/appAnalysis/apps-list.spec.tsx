import * as React from 'react';
import { MemoryRouter } from 'react-router';
import { mount } from 'enzyme';

import { WorkspaceAccessLevel } from 'generated/fetch';

import { AppsList } from 'app/pages/appAnalysis/apps-list';
import { APP_LIST } from 'app/utils/constants';
import { currentWorkspaceStore } from 'app/utils/navigation';

import { workspaceDataStub } from 'testing/stubs/workspaces';

describe('Apps list', () => {
  const startButton = (wrapper) => {
    return wrapper.find('[data-test-id="start-button"]').first();
  };

  const applicationListDropDown = (wrapper) => {
    return wrapper.find('[data-test-id="application-list-dropdown"]').first();
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
        <AppsList hideSpinner={() => {}} />
        );
      </MemoryRouter>
    );
  };

  beforeEach(() => {
    currentWorkspaceStore.next({
      ...workspaceDataStub,
      cdrVersionId: '1',
    });
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

  it('Clicking Start button should open select application modal that has list of applications', () => {
    const wrapper = component();
    startButton(wrapper).simulate('click');
    expect(
      wrapper.find('[data-test-id="select-application-modal"]')
    ).toBeTruthy();

    expect(applicationListDropDown(wrapper).prop('options')).toBe(APP_LIST);
  });
});
