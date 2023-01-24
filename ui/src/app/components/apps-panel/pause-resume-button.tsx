import { useEffect, useState } from 'react';
import * as React from 'react';
import { faPause, faPlay } from '@fortawesome/free-solid-svg-icons';
import { faSyncAlt } from '@fortawesome/free-solid-svg-icons/faSyncAlt';

import { cond, switchCase } from 'app/utils';

import { AppsPanelButton } from './apps-panel-button';
import { UserEnvironmentStatus } from './utils';

interface Props {
  externalStatus: UserEnvironmentStatus;
  onPause: Function;
  onResume: Function;
}
export const PauseResumeButton = (props: Props) => {
  const { externalStatus, onPause, onResume } = props;

  const [envStatus, setEnvStatus] =
    useState<UserEnvironmentStatus>(externalStatus);

  useEffect(() => {
    setEnvStatus(externalStatus);
  }, [externalStatus]);

  // transition from Running to Paused, or Paused to Running
  const onClick = () =>
    switchCase(
      envStatus,
      [
        'Running',
        () => {
          // transition this button immediately, instead of waiting for externalStatus updates
          setEnvStatus('Pausing');
          onPause();
        },
      ],
      [
        'Paused',
        () => {
          // transition this button immediately, instead of waiting for externalStatus updates
          setEnvStatus('Resuming');
          onResume();
        },
      ]
    );

  const [icon, buttonText, disabled] = cond(
    [envStatus === 'Pausing', () => [faSyncAlt, 'Pausing', true]],
    [envStatus === 'Resuming', () => [faSyncAlt, 'Resuming', true]],
    [envStatus === 'Paused', () => [faPlay, 'Resume', false]],
    [envStatus === 'Running', () => [faPause, 'Pause', false]],
    // choose a (disabled) default to show for other statuses
    () => [faPause, 'Pause', true]
  );

  return <AppsPanelButton {...{ icon, buttonText, disabled, onClick }} />;
};
