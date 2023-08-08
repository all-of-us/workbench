import * as React from 'react';
import { mount, ReactWrapper } from 'enzyme';

import { AppStatus } from 'generated/fetch';

import {
  cromwellConfigIconId,
  IconConfig,
  rstudioConfigIconId,
  RuntimeIcon,
} from 'app/components/help-sidebar-icons';
import { serverConfigStore, userAppsStore } from 'app/utils/stores';
import cromwellIcon from 'assets/images/Cromwell-icon.png';
import jupyterIcon from 'assets/images/Jupyter-icon.png';
import rstudioIcon from 'assets/images/RStudio-icon.png';

import {
  createListAppsCromwellResponse,
  createListAppsRStudioResponse,
} from 'testing/stubs/apps-api-stub';

import { UIAppType } from './apps-panel/utils';
import { UserAppIcon } from './help-sidebar-icons';

const getIconConfig = (iconId, label): IconConfig => {
  return {
    id: iconId,
    disabled: false,
    faIcon: null,
    label: label,
    showIcon: () => true,
    style: {},
    tooltip: '',
    hasContent: true,
  };
};

const verifySidebarIcon = (wrapper: ReactWrapper, dataTestId, src, alt) => {
  const mainIcon = wrapper.find(`[data-test-id="${dataTestId}"]`);
  expect(mainIcon.exists()).toBeTruthy();
  expect(mainIcon.prop('src')).toEqual(src);
  expect(mainIcon.prop('alt')).toEqual(alt);
};

describe('CompoundIcons', () => {
  const defaultConfig = { gsuiteDomain: 'researchallofus.org' };
  beforeEach(() => {
    serverConfigStore.set({ config: defaultConfig });
  });

  it('UserAppIcon renders for Cromwell', () => {
    const icon = cromwellIcon;
    const label = 'Cromwell Icon';
    const iconConfig = getIconConfig(cromwellConfigIconId, label);

    userAppsStore.set({
      userApps: [createListAppsCromwellResponse({ status: AppStatus.RUNNING })],
    });

    const wrapper = mount(
      <UserAppIcon
        iconConfig={iconConfig}
        userSuspended={false}
        appType={UIAppType.CROMWELL}
      />
    );

    verifySidebarIcon(
      wrapper,
      `help-sidebar-icon-${cromwellConfigIconId}`,
      icon,
      label
    );

    const statusIndicator = wrapper.find(
      `[data-test-id="app-status-icon-container"]`
    );
    expect(statusIndicator.exists()).toBeTruthy();
  });

  it('UserAppIcon renders for RStudio', () => {
    const icon = rstudioIcon;
    const label = 'RStudio Icon';
    const iconConfig = getIconConfig(rstudioConfigIconId, label);

    userAppsStore.set({
      userApps: [createListAppsRStudioResponse({ status: AppStatus.RUNNING })],
    });

    const wrapper = mount(
      <UserAppIcon
        iconConfig={iconConfig}
        userSuspended={false}
        appType={UIAppType.RSTUDIO}
      />
    );

    verifySidebarIcon(
      wrapper,
      `help-sidebar-icon-${rstudioConfigIconId}`,
      icon,
      label
    );

    const statusIndicator = wrapper.find(
      `[data-test-id="app-status-icon-container"]`
    );
    expect(statusIndicator.exists()).toBeTruthy();
  });

  test('RuntimeIcon renders', () => {
    const iconId = 'runtimeConfig';
    const label = 'Jupyter Icon';
    const iconConfig = getIconConfig(iconId, label);

    const wrapper = mount(
      <RuntimeIcon
        iconConfig={iconConfig}
        workspaceNamespace=''
        userSuspended={false}
      />
    );

    verifySidebarIcon(
      wrapper,
      `help-sidebar-icon-${iconId}`,
      jupyterIcon,
      label
    );

    const statusIndicator = wrapper.find(
      `[data-test-id="runtime-status-icon-container"]`
    );
    expect(statusIndicator.exists()).toBeTruthy();
  });
});
