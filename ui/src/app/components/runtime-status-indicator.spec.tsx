import * as React from 'react';

import { RuntimeStatus } from 'generated/fetch';

import { render, screen } from '@testing-library/react';
import {
  registerCompoundRuntimeOperation,
  runtimeStore,
} from 'app/utils/stores';

import { RuntimeApiStub } from 'testing/stubs/runtime-api-stub';

import {
  ErrorIcon,
  RunningIcon,
  StoppedIcon,
  StoppingIcon,
  UpdatingIcon,
} from './environment-status-icon';
import { RuntimeStatusIndicator } from './runtime-status-indicator';

describe(RuntimeStatusIndicator.name, () => {
  test.each([
    [RuntimeStatus.CREATING, UpdatingIcon],
    [RuntimeStatus.STOPPED, StoppedIcon],
    [RuntimeStatus.RUNNING, RunningIcon],
    [RuntimeStatus.STOPPING, StoppingIcon],
    [RuntimeStatus.ERROR, ErrorIcon],
  ])(
    'Runtime Status indicator renders correct indicator when runtime is in %s state',
    (status, icon) => {
      const runtimeStub = new RuntimeApiStub();
      runtimeStub.runtime.status = status;
      runtimeStore.set({
        workspaceNamespace: '',
        runtime: runtimeStub.runtime,
        runtimeLoaded: true,
      });
      render(<RuntimeStatusIndicator />);
      const statusIcon = screen.getByTestId(`runtime-status-icon-${status}`);
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

    render(
      <RuntimeStatusIndicator workspaceNamespace={currentWorkspaceNamespace} />
    );
    const statusIcon = screen.getByTestId('runtime-status-icon-updating');
    expect(statusIcon).toBeInTheDocument();
  });
});
