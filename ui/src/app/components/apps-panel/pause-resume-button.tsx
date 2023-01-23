import { useEffect, useState } from 'react';
import * as React from 'react';
import { faPause, faPlay } from '@fortawesome/free-solid-svg-icons';
import { faSyncAlt } from '@fortawesome/free-solid-svg-icons/faSyncAlt';

import { cond, switchCase } from 'app/utils';

import { AppsPanelButton } from './apps-panel-button';
import { EnvironmentState } from './utils';

interface Props {
  externalStatus: EnvironmentState;
  onPause: Function;
  onResume: Function;
}
export const PauseResumeButton = (props: Props) => {
  const { externalStatus, onPause, onResume } = props;

  const [envState, setEnvState] = useState<EnvironmentState>(externalStatus);

  // immediate transition states, instead of waiting for externalStatus updates
  const [pausing, setPausing] = useState(false);
  const [resuming, setResuming] = useState(false);

  // when the externalStatus is updated, also clear our transition states
  useEffect(() => {
    // if (pausing) {
    //   setPausing(false);
    // }
    // if (resuming) {
    //   setResuming(false);
    // }

    setEnvState(externalStatus);
  }, [externalStatus]);

  // transition from Running to Paused, or Paused to Running
  const onClick = () =>
    switchCase(
      envState,
      [
        'Running',
        () => {
          setEnvState('Pausing');
          onPause();
        },
      ],
      [
        'Paused',
        () => {
          setEnvState('Resuming');
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
