import * as React from 'react';
import { CSSProperties } from 'react';

import { AppStatus } from 'generated/fetch';

import {
  DeletingIcon,
  ErrorIcon,
  RunningIcon,
  StoppedIcon,
  StoppingIcon,
  SuspendedIcon,
  UpdatingIcon,
} from './environment-status-icon';
import { FlexRow } from './flex';

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
            return <UpdatingIcon />;
          case AppStatus.STOPPED:
            return <StoppedIcon />;
          case AppStatus.RUNNING:
            return <RunningIcon />;
          case AppStatus.DELETING:
            return <DeletingIcon />;
          case AppStatus.STOPPING:
            return <StoppingIcon />;
          case AppStatus.ERROR:
            return <ErrorIcon />;
        }
      })()}
    </FlexRow>
  );
};
