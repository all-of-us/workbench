import * as React from 'react';
import { faCircle, faLock, faSyncAlt } from '@fortawesome/free-solid-svg-icons';
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
    title='Icon indicating item has encountered an error'
  />
);

export const SuspendedIcon = () => (
  <FontAwesomeIcon
    icon={faLock}
    style={{
      ...styles.asyncOperationStatusIcon,
      color: colors.asyncOperationStatus.stopped,
    }}
    title='Icon indicating item is suspended'
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
    title='Icon indicating item is updating'
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
    title='Icon indicating item has stopped'
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
    title='Icon indicating item is running'
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
    title='Icon indicating item is stopping'
  />
);
