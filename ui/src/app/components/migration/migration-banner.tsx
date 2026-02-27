import * as React from 'react';

import { Button } from 'app/components/buttons';
import { FlexRow } from 'app/components/flex';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import { reactStyles } from 'app/utils';

const styles = reactStyles({
  banner: {
    background: colorWithWhiteness(colors.primary, 0.9),
    borderRadius: 6,
    padding: '16px 20px',
    marginBottom: 20,
    alignItems: 'center',
  },
  text: {
    flex: 1,
    fontWeight: 500,
  },
});

interface Props {
  onStartMigration: () => void;
}

export const MigrationBanner = ({ onStartMigration }: Props) => (
  <FlexRow style={styles.banner}>
    <div style={styles.text}>
      Migration to Verily Workbench is now open. Please migrate your workspaces
      to avoid data loss.
    </div>
    <Button onClick={onStartMigration}>Start Migration</Button>
  </FlexRow>
);
