import * as React from 'react';

import { WorkspaceRecoveryStatus } from 'generated/fetch';

import colors, { addOpacity } from 'app/styles/colors';
import { reactStyles } from 'app/utils';
const styles = reactStyles({
  badge: {
    height: '1.5rem',
    minWidth: '5.5rem',
    padding: '0 10px',
    borderRadius: '0.3rem',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    fontSize: 10,
    fontWeight: 600,
    textTransform: 'uppercase',
    letterSpacing: '.3px',
  },

  archived: {
    backgroundColor: colors.highlight,
    color: colors.warningAlt,
  },

  requested: {
    backgroundColor: '#FEF08A',
    color: '#92400E',
  },

  recovering: {
    backgroundColor: colors.light,
    color: colors.dark,
    fontSize: '9px',
  },

  recovered: {
    backgroundColor: colors.banner,
    color: colors.primary,
  },

  failed: {
    backgroundColor: addOpacity(colors.danger, 0.15).toString(),
    color: colors.danger,
  },

  waiting: {
    backgroundColor: colors.disabled,
    color: colors.white,
    fontSize: '9px',
  },
});

interface Props {
  state: WorkspaceRecoveryStatus;
}

export const RecoveryBadge = ({ state }: Props) => {
  switch (state) {
    case WorkspaceRecoveryStatus.NOT_STARTED:
      return (
        <span style={{ ...styles.badge, ...styles.archived }}>ARCHIVED</span>
      );

    case WorkspaceRecoveryStatus.REQUESTED:
      return (
        <span style={{ ...styles.badge, ...styles.requested }}>REQUESTED</span>
      );

    // Include FAILED here as users can't take any action and don't need to see a failed status. These will be handled internally
    case WorkspaceRecoveryStatus.FAILED:
    case WorkspaceRecoveryStatus.RECOVERING:
      return (
        <span style={{ ...styles.badge, ...styles.recovering }}>
          {/* Commenting out the spinner for now */}
          {/* We may want to add a status to differentiate between waiting for approval and recovery in progress */}
          {/* <FontAwesomeIcon */}
          {/*  icon={faArrowsRotate}*/}
          {/*  spin*/}
          {/*  style={{ marginRight: 6, fontSize: 10 }}*/}
          {/* /> */}
          RECOVERY PENDING
        </span>
      );

    case WorkspaceRecoveryStatus.RECOVERED:
      return (
        <span style={{ ...styles.badge, ...styles.recovered }}>RECOVERED</span>
      );

    default:
      return (
        <span style={{ ...styles.badge, ...styles.waiting }}>
          WAITING TO ARCHIVE
        </span>
      );
  }
};
