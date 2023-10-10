import * as React from 'react';
import { CSSProperties } from 'react';

import { AppStatus, RuntimeStatus } from 'generated/fetch';

import { DEFAULT, switchCase } from '@terra-ui-packages/core-utils';
import {
  toUserEnvironmentStatusByAppType,
  UIAppType,
  UserEnvironmentStatus,
} from 'app/components/apps-panel/utils';
import { Clickable } from 'app/components/buttons';
import { FlexRow } from 'app/components/flex';
import { TooltipTrigger } from 'app/components/popups';
import colors, { addOpacity } from 'app/styles/colors';
import computeError from 'assets/icons/compute-error.svg';
import computeNone from 'assets/icons/compute-none.svg';
import computeRunning from 'assets/icons/compute-running.svg';
import computeStarting from 'assets/icons/compute-starting.svg';
import computeStopped from 'assets/icons/compute-stopped.svg';
import computeStopping from 'assets/icons/compute-stopping.svg';

interface ImgProps {
  alt: string;
  src: string;
  style?: CSSProperties;
  'data-test-id': string;
}
interface ButtonProps extends ImgProps {
  onClick?: () => void;
}

interface ComponentProps {
  status: AppStatus | RuntimeStatus;
  onPause: () => void;
  onResume: () => void;
  appType: UIAppType;
}
export const StartStopEnvironmentButton = ({
  status,
  onPause,
  onResume,
  appType,
}: ComponentProps) => {
  const userEnvironmentStatus: UserEnvironmentStatus =
    toUserEnvironmentStatusByAppType(status, appType);

  const rotateStyle = { animation: 'rotation 2s infinite linear' };
  const { onClick = null, ...imgProps } = switchCase<
    UserEnvironmentStatus,
    ButtonProps
  >(
    userEnvironmentStatus,
    [
      UserEnvironmentStatus.CREATING,
      () => ({
        alt: 'Environment creation in progress',
        src: computeStarting,
        'data-test-id': 'environment-status-icon-starting',
        style: rotateStyle,
      }),
    ],
    [
      UserEnvironmentStatus.RUNNING,
      () => ({
        alt: 'Environment running, click to pause',
        src: computeRunning,
        'data-test-id': 'environment-status-icon-running',
        onClick: onPause,
      }),
    ],
    [
      UserEnvironmentStatus.UPDATING,
      () => ({
        alt: 'Environment update in progress',
        src: computeStarting,
        'data-test-id': 'environment-status-icon-starting',
        style: rotateStyle,
      }),
    ],
    [
      UserEnvironmentStatus.ERROR,
      () => ({
        alt: 'Environment in error state',
        src: computeError,
        'data-test-id': 'environment-status-icon-error',
      }),
    ],
    [
      UserEnvironmentStatus.PAUSING,
      () => ({
        alt: 'Environment pause in progress',
        src: computeStopping,
        'data-test-id': 'environment-status-icon-stopping',
        style: rotateStyle,
      }),
    ],
    [
      UserEnvironmentStatus.PAUSED,
      () => ({
        alt: 'Environment paused, click to resume',
        src: computeStopped,
        'data-test-id': 'environment-status-icon-stopped',
        onClick: onResume,
      }),
    ],
    [
      UserEnvironmentStatus.RESUMING,
      () => ({
        alt: 'Environment resume in progress',
        src: computeStarting,
        'data-test-id': 'environment-status-icon-starting',
        style: rotateStyle,
      }),
    ],
    [
      UserEnvironmentStatus.DELETING,
      () => ({
        alt: 'Environment deletion in progress',
        src: computeStopping,
        'data-test-id': 'environment-status-icon-stopping',
        style: rotateStyle,
      }),
    ],
    [
      UserEnvironmentStatus.DELETED,
      () => ({
        alt: 'Environment has been deleted',
        src: computeNone,
        'data-test-id': 'environment-status-icon-none',
      }),
    ],
    [
      UserEnvironmentStatus.UNKNOWN,
      () => ({
        alt: 'Environment status unknown',
        src: computeNone,
        'data-test-id': 'environment-status-icon-none',
      }),
    ],
    [
      DEFAULT,
      () => ({
        alt: 'No Environment found',
        src: computeNone,
        'data-test-id': 'environment-status-icon-none',
      }),
    ]
  );

  {
    /* height/width of the icon wrapper are set so that the img element can rotate inside it */
  }
  {
    /* without making it larger. the svg is 36 x 36 px, per pythagorean theorem the diagonal */
  }
  {
    /* is 50.9px, so we round up */
  }
  const iconWrapperStyle = {
    height: '51px',
    width: '51px',
    justifyContent: 'space-around',
    alignItems: 'center',
  };

  return (
    <FlexRow
      style={{
        backgroundColor: addOpacity(colors.primary, 0.1),
        justifyContent: 'space-around',
        alignItems: 'center',
        padding: '0 1.5rem',
        borderRadius: '5px 0 0 5px',
      }}
    >
      {/* TooltipTrigger inside the conditionals because it doesn't handle fragments well. */}
      {onClick && (
        <TooltipTrigger content={<div>{imgProps.alt}</div>} side='left'>
          <FlexRow style={iconWrapperStyle}>
            <Clickable {...{ onClick }} style={{ display: 'flex' }}>
              <img {...imgProps} />
            </Clickable>
          </FlexRow>
        </TooltipTrigger>
      )}
      {!onClick && (
        <TooltipTrigger content={<div>{imgProps.alt}</div>} side='left'>
          <FlexRow style={iconWrapperStyle}>
            <img {...imgProps} />
          </FlexRow>
        </TooltipTrigger>
      )}
    </FlexRow>
  );
};
