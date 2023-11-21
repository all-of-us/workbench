import '@testing-library/jest-dom';

import { AppStatus, RuntimeStatus } from 'generated/fetch';

import { render, screen, waitFor } from '@testing-library/react';
import { UIAppType } from 'app/components/apps-panel/utils';

import {
  StartStopEnvironmentButton,
  StartStopEnvironmentProps,
} from './start-stop-environment-button';

describe(StartStopEnvironmentButton.name, () => {
  const runningText = 'Environment running, click to pause';
  const pausedText = 'Environment paused, click to resume';

  const onPause = jest.fn();
  const onResume = jest.fn();
  const defaultProps: StartStopEnvironmentProps = {
    appType: UIAppType.JUPYTER,
    status: RuntimeStatus.RUNNING,
    onPause,
    onResume,
  };

  const component = async (
    propOverrides?: Partial<StartStopEnvironmentProps>
  ) =>
    render(
      <StartStopEnvironmentButton {...{ ...defaultProps, ...propOverrides }} />
    );

  describe.each([
    [UIAppType.JUPYTER, RuntimeStatus.RUNNING, RuntimeStatus.STOPPED],
    [UIAppType.CROMWELL, AppStatus.RUNNING, AppStatus.STOPPED],
    [UIAppType.RSTUDIO, AppStatus.RUNNING, AppStatus.STOPPED],
    [UIAppType.SAS, AppStatus.RUNNING, AppStatus.STOPPED],
  ])(
    '%s',
    (
      appType: UIAppType,
      runningStatus: AppStatus | RuntimeStatus,
      pausedStatus: AppStatus | RuntimeStatus
    ) => {
      it('allows pausing a running app', async () => {
        await component({ status: runningStatus, appType });
        const pauseButton = screen.getByAltText(runningText);
        pauseButton.click();
        await waitFor(() => expect(onPause).toHaveBeenCalled());
      });

      // not an expected case, but this checks that we handle it gracefully
      it('does not allow pausing a running app when onPause is not provided', async () => {
        await component({ status: runningStatus, appType, onPause: undefined });
        const pauseButton = screen.getByAltText(runningText);
        pauseButton.click();

        // clicking does nothing: onPause is not called, and we also continue to display the text

        await waitFor(() => {
          expect(onPause).not.toHaveBeenCalled();
          expect(screen.getByAltText(runningText)).toBeInTheDocument();
        });
      });

      it('allows resuming a paused app', async () => {
        await component({ status: pausedStatus, appType });
        const pauseButton = screen.getByAltText(pausedText);
        pauseButton.click();
        await waitFor(() => expect(onResume).toHaveBeenCalled());
      });

      it('does not allow resuming a paused app when onResume is not provided', async () => {
        await component({ status: pausedStatus, appType, onResume: undefined });
        const resumeButton = screen.getByAltText(pausedText);
        resumeButton.click();

        // clicking does nothing: onResume is not called, and we also continue to display the text

        await waitFor(() => {
          expect(onResume).not.toHaveBeenCalled();
          expect(screen.getByAltText(pausedText)).toBeInTheDocument();
        });
      });
    }
  );
});
