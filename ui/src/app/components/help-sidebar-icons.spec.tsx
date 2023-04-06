import * as React from 'react';
import { mount, ReactWrapper } from 'enzyme';

import { AppStatus } from 'generated/fetch';

import { IconConfig, RuntimeIcon } from 'app/components/help-sidebar-icons';
import { serverConfigStore } from 'app/utils/stores';
import thunderstorm from 'assets/icons/thunderstorm-solid.svg';
import cromwellIcon from 'assets/images/Cromwell-icon.png';
import jupyterIcon from 'assets/images/Jupyter-icon.png';

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
    const iconId = 'cromwellConfig';
    const label = 'Cromwell Icon';
    const iconConfig = getIconConfig(iconId, label);

    const wrapper = mount(
      <UserAppIcon
        iconConfig={iconConfig}
        workspaceNamespace=''
        userSuspended={false}
        status={AppStatus.RUNNING}
        appType={UIAppType.CROMWELL}
      />
    );

    verifySidebarIcon(wrapper, `help-sidebar-icon-${iconId}`, icon, label);

    const statusIndicator = wrapper.find(
      `[data-test-id="app-status-icon-container"]`
    );
    expect(statusIndicator.exists()).toBeTruthy();
  });

  test.each([
    [true, jupyterIcon],
    [false, thunderstorm],
  ])('RuntimeIcon renders when appsPanel is %s', (enableAppsPanel, icon) => {
    const iconId = 'runtimeConfig';
    const label = 'Jupyter Icon';
    const iconConfig = getIconConfig(iconId, label);

    if (enableAppsPanel) {
      serverConfigStore.set({
        config: {
          ...serverConfigStore.get().config,
          enableCromwellGKEApp: enableAppsPanel,
        },
      });
    }

    const wrapper = mount(
      <RuntimeIcon
        iconConfig={iconConfig}
        workspaceNamespace=''
        userSuspended={false}
      />
    );

    verifySidebarIcon(wrapper, `help-sidebar-icon-${iconId}`, icon, label);

    const statusIndicator = wrapper.find(
      `[data-test-id="runtime-status-icon-container"]`
    );
    expect(statusIndicator.exists()).toBeTruthy();
  });
});
