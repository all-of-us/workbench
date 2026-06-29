import * as React from 'react';
import { faArrowsRotate } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

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

  recovering: {
    backgroundColor: colors.light,
    color: colors.dark,
  },

  recovered: {
    backgroundColor: colors.banner,
    color: colors.primary,
  },

  failed: {
    backgroundColor: addOpacity(colors.danger, 0.15).toString(),
    color: colors.danger,
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

    case WorkspaceRecoveryStatus.RECOVERING:
      return (
        <span style={{ ...styles.badge, ...styles.recovering }}>
          <FontAwesomeIcon
            icon={faArrowsRotate}
            spin
            style={{ marginRight: 6, fontSize: 10 }}
          />
          RECOVERING
        </span>
      );

    case WorkspaceRecoveryStatus.RECOVERED:
      return (
        <span style={{ ...styles.badge, ...styles.recovered }}>RECOVERED</span>
      );

    case WorkspaceRecoveryStatus.FAILED:
      return <span style={{ ...styles.badge, ...styles.failed }}>FAILED</span>;

    default:
      return null;
  }
};
