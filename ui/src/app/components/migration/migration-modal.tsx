import * as React from 'react';
import { useEffect, useState } from 'react';

import { Button, CloseButton } from 'app/components/buttons';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { Modal } from 'app/components/modals';
import { workspacesApi } from 'app/services/swagger-fetch-clients';
import { reactStyles } from 'app/utils';

import { MigrationBadge } from './migration-badge';

const styles = reactStyles({
  tableHeader: {
    fontWeight: 600,
    padding: '8px 0',
    borderBottom: '1px solid #ddd',
  },
  row: {
    padding: '10px 0',
    borderBottom: '1px solid #eee',
    alignItems: 'center',
    gap: 12,
  },
  cellName: {
    flex: 2,
    minWidth: 0,
  },
  cellCdr: {
    flex: 1,
    minWidth: 0,
  },
  cellStatus: {
    flex: 2,
    minWidth: 0,
  },
  cellAction: {
    flex: '0 0 auto',
    justifyContent: 'flex-end',
  },
  header: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 12,
  },
});

interface Props {
  onClose: () => void;
}
type MigrationState = 'NOT_STARTED' | 'STARTING' | 'FINISHED';

interface MigrationWorkspace {
  id: string;
  name: string;
  cdrVersion: string | number;
  migrationState: MigrationState;
  migrationOwner?: string;
  accessLevel?: string;
}

export const MigrationModal = ({ onClose }: Props) => {
  const [workspaces, setWorkspaces] = useState<MigrationWorkspace[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const load = async () => {
      try {
        const response = await workspacesApi().getWorkspaces();
        const mapped: MigrationWorkspace[] = (response.items || []).map(
          (w: any) => {
            const migrationState = w.workspace?.migrationState ?? 'NOT_STARTED';

            return {
              id: w.workspace.namespace,
              name: w.workspace.name,
              cdrVersion: w.workspace.cdrVersionId,
              migrationState,
              migrationOwner: w.workspace?.migrationOwner,
              accessLevel: w.accessLevel,
            };
          }
        );

        setWorkspaces(mapped);
      } catch (e) {
        console.error('Failed to load workspaces', e);
      } finally {
        setLoading(false);
      }
    };

    load();
  }, []);

  const handleStartMigration = (ws: MigrationWorkspace) => {
    // stub for now
    console.log('Start migration clicked for:', ws.name);
  };

  return (
    <Modal
      title='Migrate Workspaces to Verily Workbench'
      onClose={onClose}
      width={720}
    >
      <FlexColumn>
        <FlexRow style={styles.header}>
          <div style={{ fontWeight: 600, fontSize: 18 }}>
            Migrate Workspaces to Verily Workbench
          </div>

          <CloseButton onClose={onClose} />
        </FlexRow>

        <FlexRow style={styles.tableHeader}>
          <div style={styles.cellName}>Workspace</div>
          <div style={styles.cellCdr}>CDR</div>
          <div style={styles.cellStatus}>Status</div>
          <div style={styles.cellAction} />
        </FlexRow>

        {loading ? (
          <div style={{ padding: '16px' }}>Loading workspaces…</div>
        ) : (
          workspaces.map((ws) => (
            <FlexRow key={ws.id} style={styles.row}>
              <div style={styles.cellName}>{ws.name}</div>
              <div style={styles.cellCdr}>v{ws.cdrVersion}</div>
              <div style={styles.cellStatus}>
                <MigrationBadge
                  state={ws.migrationState}
                  owner={ws.migrationOwner}
                />
              </div>
              <FlexRow style={styles.cellAction}>
                <Button
                  disabled={
                    ws.migrationState === 'STARTING' ||
                    ws.migrationState === 'FINISHED' ||
                    ws.accessLevel !== 'OWNER'
                  }
                  onClick={() => handleStartMigration(ws)}
                >
                  Start Migration
                </Button>
              </FlexRow>
            </FlexRow>
          ))
        )}
      </FlexColumn>
    </Modal>
  );
};
