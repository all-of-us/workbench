import { useEffect, useState } from 'react';
import * as React from 'react';
import { faPause, faPlay } from '@fortawesome/free-solid-svg-icons';
import { faSyncAlt } from '@fortawesome/free-solid-svg-icons/faSyncAlt';

import { cond, switchCase } from 'app/utils';

import { AppsPanelButton } from './apps-panel-button';
import { EnvironmentState } from './utils';

interface Props {
  initialState: EnvironmentState;
  onPause: Function;
  onResume: Function;
}
export const PauseResumeButton = (props: Props) => {
  const { initialState, onPause, onResume } = props;

  const [envState, setEnvState] = useState<EnvironmentState>(initialState);

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

    setEnvState(initialState);
  }, [initialState]);

  // transition from Running to Paused, or Paused to Running
  const onClick = () =>
    switchCase(
      envState,
      [
        'Running',
        () => {
          setPausing(true);
          onPause();
        },
      ],
      [
        'Paused',
        () => {
          setResuming(true);
          onResume();
        },
      ]
    );

  const [icon, buttonText, disabled] = cond(
    [pausing || envState === 'Pausing', () => [faSyncAlt, 'Pausing', true]],
    [resuming || envState === 'Resuming', () => [faSyncAlt, 'Resuming', true]],
    [envState === 'Paused', () => [faPlay, 'Resume', false]],
    [envState === 'Running', () => [faPause, 'Pause', false]],
    // choose a (disabled) default to show for other states
    () => [faPause, 'Pause', true]
  );

  return <AppsPanelButton {...{ icon, buttonText, disabled, onClick }} />;
};
