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
  style?: CSSProperties;
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
          case AppStatus.CREATING:
            return <UpdatingIcon />;
          case AppStatus.STOPPED:
            return <StoppedIcon />;
          case AppStatus.RUNNING:
          case AppStatus.READY:
            return <RunningIcon />;
          case AppStatus.STOPPING:
          case AppStatus.DELETING:
            return <StoppingIcon />;
          case AppStatus.ERROR:
          case AppStatus.BROKEN:
            return <ErrorIcon />;
        }
      })()}
    </FlexRow>
  );
};
