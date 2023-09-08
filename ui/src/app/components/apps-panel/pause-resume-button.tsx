import * as React from 'react';
import { useEffect, useState } from 'react';
import { faPause, faPlay } from '@fortawesome/free-solid-svg-icons';
import { faSyncAlt } from '@fortawesome/free-solid-svg-icons/faSyncAlt';

import { cond, switchCase } from '@terra-ui-packages/core-utils';

import { AppsPanelButton } from './apps-panel-button';
import { UserEnvironmentStatus } from './utils';

interface Props {
  externalStatus: UserEnvironmentStatus;
  onPause: Function;
  onResume: Function;
  disabled?: boolean;
  disabledTooltip?: string;
}
export const PauseResumeButton = (props: Props) => {
  const {
    externalStatus,
    onPause,
    onResume,
    disabled: disabledByProp,
    disabledTooltip,
  } = props;

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
        UserEnvironmentStatus.RUNNING,
        () => {
          // transition this button immediately, instead of waiting for externalStatus updates
          setEnvStatus(UserEnvironmentStatus.PAUSING);
          onPause();
        },
      ],
      [
        UserEnvironmentStatus.PAUSED,
        () => {
          // transition this button immediately, instead of waiting for externalStatus updates
          setEnvStatus(UserEnvironmentStatus.RESUMING);
          onResume();
        },
      ]
    );

  const [icon, buttonText, disabledByStatus] = cond(
    [
      envStatus === UserEnvironmentStatus.PAUSING,
      () => [faSyncAlt, UserEnvironmentStatus.PAUSING, true],
    ],
    [
      envStatus === UserEnvironmentStatus.RESUMING,
      () => [faSyncAlt, UserEnvironmentStatus.RESUMING, true],
    ],
    [
      envStatus === UserEnvironmentStatus.PAUSED,
      () => [faPlay, 'Resume', false],
    ],
    [
      envStatus === UserEnvironmentStatus.RUNNING,
      () => [faPause, 'Pause', false],
    ],
    // choose a (disabled) default to show for other statuses
    () => [faPause, 'Pause', true]
  );

  const disabled = disabledByProp || disabledByStatus;

  return (
    <AppsPanelButton
      {...{ icon, buttonText, disabled, onClick, disabledTooltip }}
    />
  );
};
