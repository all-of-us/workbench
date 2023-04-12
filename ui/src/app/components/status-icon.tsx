import * as React from 'react';
import { faCircle } from '@fortawesome/free-solid-svg-icons/faCircle';
import { faLock } from '@fortawesome/free-solid-svg-icons/faLock';
import { faSyncAlt } from '@fortawesome/free-solid-svg-icons/faSyncAlt';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import colors from 'app/styles/colors';
import { reactStyles } from 'app/utils';

export const styles = reactStyles({
  asyncOperationStatusIcon: {
    width: '.75rem',
    height: '.75rem',
    zIndex: 2,
  },
  runtimeStatusIconOutline: {
    border: `1px solid ${colors.white}`,
    borderRadius: '.375rem',
  },
  rotate: {
    animation: 'rotation 2s infinite linear',
  },
});

export const ErrorIcon = () => (
  <FontAwesomeIcon
    icon={faCircle}
    style={{
      ...styles.asyncOperationStatusIcon,
      ...styles.runtimeStatusIconOutline,
      color: colors.asyncOperationStatus.error,
    }}
  />
);

export const SuspendedIcon = () => (
  <FontAwesomeIcon
    icon={faLock}
    style={{
      ...styles.asyncOperationStatusIcon,
      color: colors.asyncOperationStatus.stopped,
    }}
  />
);

export const UpdatingIcon = () => (
  <FontAwesomeIcon
    icon={faSyncAlt}
    style={{
      ...styles.asyncOperationStatusIcon,
      ...styles.rotate,
      color: colors.asyncOperationStatus.starting,
    }}
  />
);

export const StoppedIcon = () => (
  <FontAwesomeIcon
    icon={faCircle}
    style={{
      ...styles.asyncOperationStatusIcon,
      ...styles.runtimeStatusIconOutline,
      color: colors.asyncOperationStatus.stopped,
    }}
  />
);

export const RunningIcon = () => (
  <FontAwesomeIcon
    icon={faCircle}
    style={{
      ...styles.asyncOperationStatusIcon,
      ...styles.runtimeStatusIconOutline,
      color: colors.asyncOperationStatus.running,
    }}
  />
);

export const StoppingIcon = () => (
  <FontAwesomeIcon
    icon={faSyncAlt}
    style={{
      ...styles.asyncOperationStatusIcon,
      ...styles.rotate,
      color: colors.asyncOperationStatus.stopping,
    }}
  />
);
