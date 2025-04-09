import * as React from 'react';
import { mockNavigate } from 'setupTests';

import { AppsApi, AppStatus, UserAppEnvironment } from 'generated/fetch';

import { render, screen, waitFor } from '@testing-library/react';
import userEvent, { UserEvent } from '@testing-library/user-event';
import { UIAppType } from 'app/components/apps-panel/utils';
import { appDisplayPath } from 'app/routing/utils';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
import { serverConfigStore } from 'app/utils/stores';

import defaultServerConfig from 'testing/default-server-config';
import {
  expectButtonElementDisabled,
  expectButtonElementEnabled,
} from 'testing/react-test-helpers';
import {
  AppsApiStub,
  createListAppsRStudioResponse,
  createListAppsSASResponse,
} from 'testing/stubs/apps-api-stub';
import {
  workspaceStubs,
  WorkspaceStubVariables,
} from 'testing/stubs/workspaces';

import { OpenGkeAppButton, OpenGkeAppButtonProps } from './open-gke-app-button';

describe(OpenGkeAppButton.name, () => {
  const defaultProps: OpenGkeAppButtonProps = {
    userApp: null,
    workspace: workspaceStubs[0],
    onClose: () => {},
  };

  let user: UserEvent;

  const component = async (propOverrides?: Partial<OpenGkeAppButtonProps>) => {
    const allProps = { ...defaultProps, ...propOverrides };
    return render(<OpenGkeAppButton {...allProps} />);
  };

  const findOpenButton = (appType: string) =>
    screen.getByRole('button', {
      name: `${appType} cloud environment open button`,
    });

  beforeEach(() => {
    registerApiClient(AppsApi, new AppsApiStub());
    user = userEvent.setup();
    serverConfigStore.set({ config: defaultServerConfig });
  });
  afterEach(() => {
    jest.resetAllMocks();
  });

  describe.each([
    [
      UIAppType.RSTUDIO,
      { ...createListAppsRStudioResponse(), status: AppStatus.RUNNING },
    ],
    [
      UIAppType.SAS,
      { ...createListAppsSASResponse(), status: AppStatus.RUNNING },
    ],
  ])('%s', (appType: UIAppType, userApp: UserAppEnvironment) => {
    it(`should allow opening a running ${appType} app`, async () => {
      const onClose = jest.fn();
      await component({ userApp, onClose });

      const button = await waitFor(() => {
        const openButton = findOpenButton(appType);
        expectButtonElementEnabled(openButton);
        return openButton;
      });

      button.click();
      await waitFor(() => {
        expect(mockNavigate).toHaveBeenCalledWith([
          appDisplayPath(
            WorkspaceStubVariables.DEFAULT_WORKSPACE_NS,
            WorkspaceStubVariables.DEFAULT_WORKSPACE_TERRA_NAME,
            appType
          ),
        ]);
      });

      await waitFor(() => expect(onClose).toHaveBeenCalled());
    });

    it(`should not allow creating a running ${appType} app when billing is exhausted.`, async () => {
      await component({
        userApp,
        workspace: {
          ...workspaceStubs[0],
          initialCredits: { exhausted: true },
        },
      });
      const button = await waitFor(() => {
        const openButton = findOpenButton(appType);
        expectButtonElementDisabled(openButton);
        return openButton;
      });

      await user.pointer([{ pointerName: 'mouse', target: button }]);

      await screen.findByText(
        'You have either run out of initial credits or have an inactive billing account.'
      );
    });
  });
});
