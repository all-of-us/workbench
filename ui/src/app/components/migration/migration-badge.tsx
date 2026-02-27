import * as React from 'react';

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
type MigrationState = 'NOT_STARTED' | 'STARTING' | 'FINISHED';

interface Props {
  state: MigrationState;
  owner?: string;
}

export const MigrationBadge = ({ state, owner }: Props) => {
  const getLabel = () => {
    switch (state) {
      case 'NOT_STARTED':
        return 'Ready to migrate';

      case 'STARTING':
        return owner
          ? `Migration in progress (Initiated by ${owner})`
          : 'Migration in progress';

      case 'FINISHED':
        return 'Migrated';

      default:
        return '';
    }
  };

  const getStyle = () => {
    switch (state) {
      case 'NOT_STARTED':
        return {
          background: colorWithWhiteness(colors.success, 0.85),
          color: colors.success,
        };

      case 'STARTING':
        return {
          background: colorWithWhiteness(colors.primary, 0.85),
          color: colors.primary,
        };

      case 'FINISHED':
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
