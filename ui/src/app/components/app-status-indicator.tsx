import * as React from 'react';
import { CSSProperties } from 'react';

import { AppStatus } from 'generated/fetch';

import { FlexRow } from './flex';
import {
  ErrorIcon,
  RunningIcon,
  StoppedIcon,
  StoppingIcon,
  SuspendedIcon,
  UpdatingIcon,
} from './status-icon';

export const AppStatusIndicator = (props: {
  style: CSSProperties;
  userSuspended: boolean;
  appStatus: AppStatus;
}) => {
  const { appStatus, style, userSuspended } = props;

  return (
    <FlexRow {...{ style }} data-test-id='app-status-icon-container'>
      {(() => {
        if (userSuspended) {
          return <SuspendedIcon />;
        }
        switch (appStatus) {
          case AppStatus.STARTING:
          case AppStatus.PROVISIONING:
            return <UpdatingIcon />;
          case AppStatus.STOPPED:
            return <StoppedIcon />;
          case AppStatus.RUNNING:
            return <RunningIcon />;
          case AppStatus.STOPPING:
          case AppStatus.DELETING:
            return <StoppingIcon />;
          case AppStatus.ERROR:
            return <ErrorIcon />;
        }
      })()}
    </FlexRow>
  );
};
