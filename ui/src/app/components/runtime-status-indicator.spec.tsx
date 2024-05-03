import '@testing-library/jest-dom';

import * as React from 'react';

import { RuntimeStatus } from 'generated/fetch';

import { render, screen, within } from '@testing-library/react';
import {
  registerCompoundRuntimeOperation,
  runtimeStore,
} from 'app/utils/stores';

import { RuntimeApiStub } from 'testing/stubs/runtime-api-stub';

import { RuntimeStatusIndicator } from './runtime-status-indicator';

describe(RuntimeStatusIndicator.name, () => {
  test.each([
    [RuntimeStatus.CREATING, 'is updating'],
    [RuntimeStatus.STOPPED, 'has stopped'],
    [RuntimeStatus.RUNNING, 'is running'],
    [RuntimeStatus.STOPPING, 'is stopping'],
    [RuntimeStatus.ERROR, 'has encountered an error'],
  ])(
    'Runtime Status indicator renders correct indicator when runtime is in %s state',
    (status, iconMeaning) => {
      const runtimeStub = new RuntimeApiStub();
      runtimeStub.runtime.status = status;
      runtimeStore.set({
        workspaceNamespace: '',
        runtime: runtimeStub.runtime,
        runtimeLoaded: true,
      });
      const { getByTestId } = render(<RuntimeStatusIndicator />);

      const iconContainer = getByTestId('runtime-status-icon-container');
      const statusIcon = within(iconContainer).getByTitle(
        `Icon indicating environment ${iconMeaning}`
      );
      expect(statusIcon).toBeInTheDocument();
    }
  );

  it('Verify that a runtime with an undefined status does not have a status indicator', () => {
    const runtimeStub = new RuntimeApiStub();
    runtimeStub.runtime.status = undefined;
    runtimeStore.set({
      workspaceNamespace: '',
      runtime: runtimeStub.runtime,
      runtimeLoaded: true,
    });
    render(<RuntimeStatusIndicator />);
    const iconContainer = screen.getByTestId('runtime-status-icon-container');
    expect(iconContainer).toBeInTheDocument();
    expect(iconContainer.children.length).toEqual(0);
  });

  it('Verify that a runtime that is part of a compound runtimeop is shown as updating', () => {
    const currentWorkspaceNamespace = 'testNamespace';
    const runtimeStub = new RuntimeApiStub();
    runtimeStub.runtime.status = undefined;
    runtimeStore.set({
      workspaceNamespace: currentWorkspaceNamespace,
      runtime: runtimeStub.runtime,
      runtimeLoaded: true,
    });
    const aborter = new AbortController();
    registerCompoundRuntimeOperation(currentWorkspaceNamespace, {
      pendingRuntime: runtimeStub.runtime,
      aborter,
    });

    const { getByTestId } = render(
      <RuntimeStatusIndicator workspaceNamespace={currentWorkspaceNamespace} />
    );

    const iconContainer = getByTestId('runtime-status-icon-container');
    const statusIcon = within(iconContainer).getByTitle(
      'Icon indicating environment is updating'
    );
    expect(statusIcon).toBeInTheDocument();
  });
});
