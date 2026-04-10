import * as React from 'react';

import { MigrationState } from 'generated/fetch';

import { reactStyles } from 'app/utils';

const styles = reactStyles({
  badge: {
    height: '1.5rem',
    fontSize: 10,
    borderRadius: '0.3rem',
    padding: '0 10px',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    minWidth: '4.5rem',
  },
  inProgress: {
    backgroundColor: '#E5E7EB', // light grey
    color: '#374151',
  },
  completed: {
    backgroundColor: '#C7E6E3', // teal
    color: '#1F4D4A',
  },
  failed: {
    backgroundColor: '#FEE2E2',
    color: '#991B1B',
  },
});

interface Props {
  state: MigrationState;
}

export const MigrationBadge = ({ state }: Props) => {
  if (state === MigrationState.STARTING) {
    return (
      <span style={{ ...styles.badge, ...styles.inProgress }}>IN PROGRESS</span>
    );
  }

  if (state === MigrationState.FINISHED) {
    return (
      <span style={{ ...styles.badge, ...styles.completed }}>MIGRATED</span>
    );
  }

  if (state === MigrationState.FAILED) {
    return <span style={{ ...styles.badge, ...styles.failed }}>FAILED</span>;
  }

  return null;
};
