import { MigrationState } from 'generated/fetch';

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
  onStartMigration?: () => void;
  state?: MigrationState;
}

export const MigrationBanner = ({ onStartMigration, state }: Props) => {
  //  Homepage banner (CTA only)
  if (!state) {
    return (
      <FlexRow style={styles.banner}>
        <div style={styles.text}>
          Migration to Verily Workbench is now available. Please migrate your
          workspaces to avoid potential data loss.
        </div>
        {onStartMigration && (
          <Button onClick={onStartMigration}>Migrate Workspace</Button>
        )}
      </FlexRow>
    );
  }

  // Workspace 1.0 banner (ONLY when FINISHED)
  if (state === MigrationState.FINISHED) {
    return (
      <FlexRow style={styles.banner}>
        <div style={styles.text}>
          <strong>
            This workspace has already been migrated to Verily Workbench.
          </strong>{' '}
          Any new changes made here will <strong>NOT</strong> automatically
          reflect in the migrated workspace. Re-migration is required to sync
          updates. To avoid ongoing Google Cloud charges, please delete this
          Workspace 1.0 instance.
        </div>
      </FlexRow>
    );
  }
  return null;
};
