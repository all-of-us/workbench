import * as React from 'react';

import { RuntimeStatus } from 'generated/fetch';

import { Clickable } from 'app/components/buttons';
import { FlexRow } from 'app/components/flex';
import { TooltipTrigger } from 'app/components/popups';
import colors, { addOpacity } from 'app/styles/colors';
import { DEFAULT, switchCase } from 'app/utils';
import {
  RuntimeStatusRequest,
  useRuntimeStatus,
} from 'app/utils/runtime-utils';
import computeError from 'assets/icons/compute-error.svg';
import computeNone from 'assets/icons/compute-none.svg';
import computeRunning from 'assets/icons/compute-running.svg';
import computeStarting from 'assets/icons/compute-starting.svg';
import computeStopped from 'assets/icons/compute-stopped.svg';
import computeStopping from 'assets/icons/compute-stopping.svg';

export const StartStopRuntimeButton = ({
  workspaceNamespace,
  googleProject,
}) => {
  const [status, setRuntimeStatus] = useRuntimeStatus(
    workspaceNamespace,
    googleProject
  );

  const rotateStyle = { animation: 'rotation 2s infinite linear' };
  const {
    altText,
    iconSrc,
    dataTestId,
    styleOverrides = {},
    onClick = null,
  } = switchCase(
    status,
    [
      RuntimeStatus.Creating,
      () => ({
        altText: 'Runtime creation in progress',
        iconSrc: computeStarting,
        dataTestId: 'runtime-status-icon-starting',
        styleOverrides: rotateStyle,
      }),
    ],
    [
      RuntimeStatus.Running,
      () => ({
        altText: 'Runtime running, click to pause',
        iconSrc: computeRunning,
        dataTestId: 'runtime-status-icon-running',
        onClick: () => {
          setRuntimeStatus(RuntimeStatusRequest.Stop);
        },
      }),
    ],
    [
      RuntimeStatus.Updating,
      () => ({
        altText: 'Runtime update in progress',
        iconSrc: computeStarting,
        dataTestId: 'runtime-status-icon-starting',
        styleOverrides: rotateStyle,
      }),
    ],
    [
      RuntimeStatus.Error,
      () => ({
        altText: 'Runtime in error state',
        iconSrc: computeError,
        dataTestId: 'runtime-status-icon-error',
      }),
    ],
    [
      RuntimeStatus.Stopping,
      () => ({
        altText: 'Runtime pause in progress',
        iconSrc: computeStopping,
        dataTestId: 'runtime-status-icon-stopping',
        styleOverrides: rotateStyle,
      }),
    ],
    [
      RuntimeStatus.Stopped,
      () => ({
        altText: 'Runtime paused, click to resume',
        iconSrc: computeStopped,
        dataTestId: 'runtime-status-icon-stopped',
        onClick: () => {
          setRuntimeStatus(RuntimeStatusRequest.Start);
        },
      }),
    ],
    [
      RuntimeStatus.Starting,
      () => ({
        altText: 'Runtime resume in progress',
        iconSrc: computeStarting,
        dataTestId: 'runtime-status-icon-starting',
        styleOverrides: rotateStyle,
      }),
    ],
    [
      RuntimeStatus.Deleting,
      () => ({
        altText: 'Runtime deletion in progress',
        iconSrc: computeStopping,
        dataTestId: 'runtime-status-icon-stopping',
        styleOverrides: rotateStyle,
      }),
    ],
    [
      RuntimeStatus.Deleted,
      () => ({
        altText: 'Runtime has been deleted',
        iconSrc: computeNone,
        dataTestId: 'runtime-status-icon-none',
      }),
    ],
    [
      RuntimeStatus.Unknown,
      () => ({
        altText: 'Runtime status unknown',
        iconSrc: computeNone,
        dataTestId: 'runtime-status-icon-none',
      }),
    ],
    [
      DEFAULT,
      () => ({
        altText: 'No runtime found',
        iconSrc: computeNone,
        dataTestId: 'runtime-status-icon-none',
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
        padding: '0 1rem',
        borderRadius: '5px 0 0 5px',
      }}
    >
      {/* TooltipTrigger inside the conditionals because it doesn't handle fragments well. */}
      {onClick && (
        <TooltipTrigger content={<div>{altText}</div>} side='left'>
          <FlexRow style={iconWrapperStyle}>
            <Clickable onClick={() => onClick()}>
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
