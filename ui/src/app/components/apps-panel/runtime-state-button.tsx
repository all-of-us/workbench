import * as React from 'react';
import { useEffect, useState } from 'react';
import { faPause, faPlay } from '@fortawesome/free-solid-svg-icons';
import { faSyncAlt } from '@fortawesome/free-solid-svg-icons/faSyncAlt';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { RuntimeStatus, Workspace } from 'generated/fetch';

import { Clickable } from 'app/components/buttons';
import { FlexColumn } from 'app/components/flex';
import { cond, switchCase } from 'app/utils';
import {
  RuntimeStatusRequest,
  useRuntimeStatus,
} from 'app/utils/runtime-utils';

import { buttonStyles } from './utils';

export const RuntimeStateButton = (props: { workspace: Workspace }) => {
  const {
    workspace: { namespace, googleProject },
  } = props;

  const [status, setRuntimeStatus] = useRuntimeStatus(namespace, googleProject);

  // transition states, so we don't have to wait for polling
  const [pausing, setPausing] = useState(false);
  const [resuming, setResuming] = useState(false);

  // when polling updates the status value, clear our transition states
  useEffect(() => {
    if (pausing) {
      setPausing(false);
    }
    if (resuming) {
      setResuming(false);
    }
  }, [status]);

  // transition from RUNNING to STOPPED, or STOPPED to RUNNING
  const toggleRuntimeStatus = () =>
    switchCase(
      status,
      [
        RuntimeStatus.Running,
        () => {
          setPausing(true);
          setRuntimeStatus(RuntimeStatusRequest.Stop);
        },
      ],
      [
        RuntimeStatus.Stopped,
        () => {
          setResuming(true);
          setRuntimeStatus(RuntimeStatusRequest.Start);
        },
      ]
    );

  const [icon, buttonText, enabled] = cond(
    [
      pausing || status === RuntimeStatus.Stopping,
      () => [faSyncAlt, 'Pausing', false],
    ],
    [
      resuming || status === RuntimeStatus.Starting,
      () => [faSyncAlt, 'Resuming', false],
    ],
    [status === RuntimeStatus.Stopped, () => [faPlay, 'Resume', true]],
    [status === RuntimeStatus.Running, () => [faPause, 'Pause', true]],
    // choose a (disabled) default to show for other states
    () => [faPause, 'Pause', false]
  );

  return (
    <Clickable
      disabled={!enabled}
      style={{ padding: '0.5em' }}
      onClick={toggleRuntimeStatus}
    >
      <FlexColumn
        style={enabled ? buttonStyles.button : buttonStyles.disabledButton}
      >
        <FontAwesomeIcon {...{ icon }} style={buttonStyles.buttonIcon} />
        <div
          style={
            enabled ? buttonStyles.buttonText : buttonStyles.disabledButtonText
          }
        >
          {buttonText}
        </div>
      </FlexColumn>
    </Clickable>
  );
};
