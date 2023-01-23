import { useEffect, useState } from 'react';
import * as React from 'react';
import { faPause, faPlay } from '@fortawesome/free-solid-svg-icons';
import { faSyncAlt } from '@fortawesome/free-solid-svg-icons/faSyncAlt';

import { RuntimeStatus } from 'generated/fetch';

import { cond, switchCase } from 'app/utils';

import { AppsPanelButton } from './apps-panel-button';

type EnvironmentState =
  | 'UNINITIALIZED'
  | 'running'
  | 'pausing'
  | 'paused'
  | 'resuming';

const toEnvState = (status: RuntimeStatus): EnvironmentState =>
  cond(
    [status === RuntimeStatus.Running, () => 'running'],
    [status === RuntimeStatus.Stopping, () => 'pausing'],
    [status === RuntimeStatus.Stopped, () => 'paused'],
    [status === RuntimeStatus.Starting, () => 'resuming'],
    () => 'UNINITIALIZED'
  );

interface Props {
  externalStatus: RuntimeStatus;
  onPause: Function;
  onResume: Function;
}
export const PauseResumeButton = (props: Props) => {
  const { externalStatus, onPause, onResume } = props;

  const [envState, setEnvState] = useState<EnvironmentState>('UNINITIALIZED');

  // immediate transition states, instead of waiting for external updates
  const [pausing, setPausing] = useState(false);
  const [resuming, setResuming] = useState(false);

  // when the external state is updated, also clear our transition states
  useEffect(() => {
    if (pausing) {
      setPausing(false);
    }
    if (resuming) {
      setResuming(false);
    }

    setEnvState(toEnvState(externalStatus));
  }, [externalStatus]);

  // transition from RUNNING to STOPPED, or STOPPED to RUNNING
  const onClick = () =>
    switchCase(
      envState,
      [
        'running',
        () => {
          setPausing(true);
          onPause();
        },
      ],
      [
        'paused',
        () => {
          setResuming(true);
          onResume();
        },
      ]
    );

  const [icon, buttonText, disabled] = cond(
    [pausing || envState === 'pausing', () => [faSyncAlt, 'Pausing', true]],
    [resuming || envState === 'resuming', () => [faSyncAlt, 'Resuming', true]],
    [envState === 'paused', () => [faPlay, 'Resume', false]],
    [envState === 'running', () => [faPause, 'Pause', false]],
    // choose a (disabled) default to show for other states
    () => [faPause, 'Pause', true]
  );

  return <AppsPanelButton {...{ icon, buttonText, disabled, onClick }} />;
};
