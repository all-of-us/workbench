import * as React from 'react';

import { MigrationState } from 'generated/fetch';

import colors, { colorWithWhiteness } from 'app/styles/colors';
import { reactStyles } from 'app/utils';

const styles = reactStyles({
  badge: {
    padding: '4px 8px',
    borderRadius: 12,
    fontSize: 12,
    whiteSpace: 'normal',
    fontWeight: 600,
  },
});

interface Props {
  state: MigrationState;
  owner?: string;
}

export const MigrationBadge = ({ state, owner }: Props) => {
  const getLabel = () => {
    switch (state) {
      case MigrationState.NOT_STARTED:
        return 'Ready to migrate';

      case MigrationState.STARTING:
        return owner
          ? `Migration in progress (Initiated by ${owner})`
          : 'Migration in progress';

      case MigrationState.FINISHED:
        return 'Migrated';

      default:
        return '';
    }
  };

  const getStyle = () => {
    switch (state) {
      case MigrationState.NOT_STARTED:
        return {
          background: colorWithWhiteness(colors.success, 0.85),
          color: colors.success,
        };

      case MigrationState.STARTING:
        return {
          background: colorWithWhiteness(colors.primary, 0.85),
          color: colors.primary,
        };

      case MigrationState.FINISHED:
        return {
          background: colorWithWhiteness(colors.secondary, 0.85),
          color: colors.secondary,
        };

      default:
        return {};
    }
  };

  return <span style={{ ...styles.badge, ...getStyle() }}>{getLabel()}</span>;
};
