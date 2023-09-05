import * as React from 'react';

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

interface StatusInfo {
  status: AppStatus | RuntimeStatus;
  onPause: () => Promise<any>;
  onResume: () => Promise<any>;
  appType: UIAppType;
}

export const StartStopEnvironmentButton = ({
  status,
  onPause,
  onResume,
  appType,
}: StatusInfo) => {
  const userEnvironmentStatus: UserEnvironmentStatus =
    toUserEnvironmentStatusByAppType(status, appType);

  const rotateStyle = { animation: 'rotation 2s infinite linear' };
  const {
    altText,
    iconSrc,
    dataTestId,
    styleOverrides = {},
    onClick = null,
  } = switchCase<any, any>(
    userEnvironmentStatus,
    [
      UserEnvironmentStatus.CREATING,
      () => ({
        altText: 'Environment creation in progress',
        iconSrc: computeStarting,
        dataTestId: 'environment-status-icon-starting',
        styleOverrides: rotateStyle,
      }),
    ],
    [
      UserEnvironmentStatus.RUNNING,
      () => ({
        altText: 'Environment running, click to pause',
        iconSrc: computeRunning,
        dataTestId: 'environment-status-icon-running',
        onClick: onPause,
      }),
    ],
    [
      UserEnvironmentStatus.UPDATING,
      () => ({
        altText: 'Environment update in progress',
        iconSrc: computeStarting,
        dataTestId: 'environment-status-icon-starting',
        styleOverrides: rotateStyle,
      }),
    ],
    [
      UserEnvironmentStatus.ERROR,
      () => ({
        altText: 'Environment in error state',
        iconSrc: computeError,
        dataTestId: 'environment-status-icon-error',
      }),
    ],
    [
      UserEnvironmentStatus.PAUSING,
      () => ({
        altText: 'Environment pause in progress',
        iconSrc: computeStopping,
        dataTestId: 'environment-status-icon-stopping',
        styleOverrides: rotateStyle,
      }),
    ],
    [
      UserEnvironmentStatus.PAUSED,
      () => ({
        altText: 'Environment paused, click to resume',
        iconSrc: computeStopped,
        dataTestId: 'environment-status-icon-stopped',
        onClick: onResume,
      }),
    ],
    [
      UserEnvironmentStatus.RESUMING,
      () => ({
        altText: 'Environment resume in progress',
        iconSrc: computeStarting,
        dataTestId: 'environment-status-icon-starting',
        styleOverrides: rotateStyle,
      }),
    ],
    [
      UserEnvironmentStatus.DELETING,
      () => ({
        altText: 'Environment deletion in progress',
        iconSrc: computeStopping,
        dataTestId: 'environment-status-icon-stopping',
        styleOverrides: rotateStyle,
      }),
    ],
    [
      UserEnvironmentStatus.DELETED,
      () => ({
        altText: 'Environment has been deleted',
        iconSrc: computeNone,
        dataTestId: 'environment-status-icon-none',
      }),
    ],
    [
      UserEnvironmentStatus.UNKNOWN,
      () => ({
        altText: 'Environment status unknown',
        iconSrc: computeNone,
        dataTestId: 'environment-status-icon-none',
      }),
    ],
    [
      DEFAULT,
      () => ({
        altText: 'No Environment found',
        iconSrc: computeNone,
        dataTestId: 'environment-status-icon-none',
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
        <TooltipTrigger content={<div>{altText}</div>} side='left'>
          <FlexRow style={iconWrapperStyle}>
            <Clickable {...{ onClick }} style={{ display: 'flex' }}>
              <img
                alt={altText}
                src={iconSrc}
                style={styleOverrides}
                data-test-id={dataTestId}
              />
            </Clickable>
          </FlexRow>
        </TooltipTrigger>
      )}
      {!onClick && (
        <TooltipTrigger content={<div>{altText}</div>} side='left'>
          <FlexRow style={iconWrapperStyle}>
            <img
              alt={altText}
              src={iconSrc}
              style={styleOverrides}
              data-test-id={dataTestId}
            />
          </FlexRow>
        </TooltipTrigger>
      )}
    </FlexRow>
  );
};
