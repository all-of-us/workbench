import '@testing-library/jest-dom';

import * as React from 'react';

import { AppStatus } from 'generated/fetch';

import { render, screen } from '@testing-library/react';
import {
  cromwellConfigIconId,
  IconConfig,
  rstudioConfigIconId,
  RuntimeIcon,
  sasConfigIconId,
} from 'app/components/help-sidebar-icons';
import { serverConfigStore, userAppsStore } from 'app/utils/stores';

import {
  createListAppsCromwellResponse,
  createListAppsRStudioResponse,
  createListAppsSASResponse,
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

describe('CompoundIcons', () => {
  const defaultConfig = { gsuiteDomain: 'researchallofus.org' };
  beforeEach(() => {
    serverConfigStore.set({ config: defaultConfig });
  });

  it('UserAppIcon renders for Cromwell', async () => {
    const label = 'Cromwell Icon';
    const iconConfig = getIconConfig(cromwellConfigIconId, label);

    userAppsStore.set({
      userApps: [createListAppsCromwellResponse({ status: AppStatus.RUNNING })],
    });

    const { container } = render(
      <UserAppIcon
        iconConfig={iconConfig}
        userSuspended={false}
        appType={UIAppType.CROMWELL}
      />
    );

    const iconElement = screen.getByLabelText(label);
    expect(iconElement).toBeInTheDocument();

    const statusIndicator = container.querySelector(
      `[data-test-id="app-status-icon-container"]`
    );
    expect(statusIndicator).toBeInTheDocument();
  });

  it('UserAppIcon renders for RStudio', async () => {
    const label = 'RStudio Icon';
    const iconConfig = getIconConfig(rstudioConfigIconId, label);

    userAppsStore.set({
      userApps: [createListAppsRStudioResponse({ status: AppStatus.RUNNING })],
    });

    const { container } = render(
      <UserAppIcon
        iconConfig={iconConfig}
        userSuspended={false}
        appType={UIAppType.RSTUDIO}
      />
    );

    const iconElement = screen.getByLabelText(label);
    expect(iconElement).toBeInTheDocument();

    const statusIndicator = container.querySelector(
      `[data-test-id="app-status-icon-container"]`
    );
    expect(statusIndicator).toBeInTheDocument();
  });

  it('UserAppIcon renders for SAS', async () => {
    const label = 'SAS Icon';
    const iconConfig = getIconConfig(sasConfigIconId, label);

    userAppsStore.set({
      userApps: [createListAppsSASResponse({ status: AppStatus.RUNNING })],
    });

    const { container } = render(
      <UserAppIcon
        iconConfig={iconConfig}
        userSuspended={false}
        appType={UIAppType.SAS}
      />
    );

    const iconElement = screen.getByLabelText(label);
    expect(iconElement).toBeInTheDocument();

    const statusIndicator = container.querySelector(
      `[data-test-id="app-status-icon-container"]`
    );
    expect(statusIndicator).toBeInTheDocument();
  });

  test('RuntimeIcon renders', () => {
    const iconId = 'runtimeConfig';
    const label = 'Jupyter Icon';
    const iconConfig = getIconConfig(iconId, label);

    const { container } = render(
      <RuntimeIcon
        iconConfig={iconConfig}
        workspaceNamespace=''
        userSuspended={false}
      />
    );

    const iconElement = screen.getByLabelText(label);
    expect(iconElement).toBeInTheDocument();

    const statusIndicator = container.querySelector(
      `[data-test-id="runtime-status-icon-container"]`
    );
    expect(statusIndicator).toBeInTheDocument();
  });
});
