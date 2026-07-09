import * as React from 'react';
import { useState } from 'react';

import { Workspace, WorkspaceRecoveryStatus } from 'generated/fetch';

import { Button } from 'app/components/buttons';
import {
  Modal,
  ModalBody,
  ModalFooter,
  ModalTitle,
} from 'app/components/modals';
import { SpinnerOverlay } from 'app/components/spinners';
import { workspacesApi } from 'app/services/swagger-fetch-clients';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import { reactStyles } from 'app/utils';

const styles = reactStyles({
  modalBody: {
    display: 'flex',
    flexDirection: 'column',
  },

  grid: {
    display: 'grid',
    gridTemplateColumns: '1fr 1fr',
    gap: '1rem',
    marginBottom: '1.5rem',
  },

  fieldLabel: {
    fontSize: '12px',
    fontWeight: 600,
    color: colors.primary,
    marginBottom: '0.35rem',
  },

  fieldValue: {
    background: colors.white,
    border: `1px solid ${colorWithWhiteness(colors.dark, 0.88)}`,
    borderRadius: '4px',
    padding: '0.6rem 0.75rem',
    color: colors.dark,
    fontSize: '13px',
    minHeight: '2.4rem',
    display: 'flex',
    alignItems: 'center',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
  },

  statusBadge: {
    background: colors.white,
    border: `1px solid ${colorWithWhiteness(colors.dark, 0.88)}`,
    borderRadius: '4px',
    padding: '0.6rem 0.75rem',
    display: 'flex',
    alignItems: 'center',
    minHeight: '2.4rem',
    gap: '0.5rem',
  },

  statusText: {
    color: colors.dark,
    fontSize: '13px',
  },

  divider: {
    borderTop: `1px solid ${colorWithWhiteness(colors.dark, 0.88)}`,
    margin: '1rem 0 1.5rem',
  },

  podSection: {
    display: 'flex',
    flexDirection: 'column',
    gap: '0.75rem',
  },

  podRow: {
    display: 'flex',
    alignItems: 'center',
    gap: '0.75rem',
  },

  sectionLabel: {
    fontSize: '12px',
    fontWeight: 600,
    color: colors.primary,
  },
});

const ReadOnlyField = ({ label, value }: { label: string; value?: string }) => (
  <div>
    <span style={styles.fieldLabel}>{label}</span>

    <div style={styles.fieldValue} title={value || 'N/A'}>
      {value || 'N/A'}
    </div>
  </div>
);

const statusDotStyle = (status: WorkspaceRecoveryStatus) => {
  let background = colors.disabled;

  switch (status) {
    case WorkspaceRecoveryStatus.RECOVERING:
      background = colors.warning;
      break;

    case WorkspaceRecoveryStatus.RECOVERED:
      background = colors.success;
      break;

    case WorkspaceRecoveryStatus.FAILED:
      background = colors.danger;
      break;
  }

  return {
    width: '8px',
    height: '8px',
    borderRadius: '50%',
    background,
    flexShrink: 0,
  };
};

const StatusField = ({
  label,
  status,
}: {
  label: string;
  status: WorkspaceRecoveryStatus;
}) => {
  const display = status
    .replace(/_/g, ' ')
    .toLowerCase()
    .replace(/^\w/, (c) => c.toUpperCase());

  return (
    <div>
      <span style={styles.fieldLabel}>{label}</span>

      <div style={styles.statusBadge}>
        <span style={statusDotStyle(status)} />
        <span style={styles.statusText}>{display}</span>
      </div>
    </div>
  );
};

export const AdminWorkspaceRecoveryModal = ({
  workspace,
  onClose,
  reload,
}: {
  workspace: Workspace;
  onClose: () => void;
  reload: () => void;
}) => {
  const [startingRecovery, setStartingRecovery] = useState(false);

  const handleRecovery = async () => {
    if (!workspace) {
      return;
    }

    try {
      setStartingRecovery(true);
      await workspacesApi().startWorkspaceRecovery(
        workspace.namespace,
        workspace.terraName
      );

      await reload();
      onClose();
    } catch (e) {
      console.error(e);
    } finally {
      setStartingRecovery(false);
    }
  };

  const recoveryButtonLabel = startingRecovery
    ? 'Starting...'
    : workspace?.recoveryState === WorkspaceRecoveryStatus.RECOVERING
    ? 'Recovery in progress'
    : 'Start recovery';

  return (
    <Modal
      data-test-id='workspace-archive-recovery-modal'
      aria={{ label: 'Workspace Archive Recovery' }}
      width={650}
    >
      <ModalTitle>Workspace archive recovery</ModalTitle>

      <ModalBody>
        <div style={styles.modalBody}>
          <div style={styles.grid}>
            <ReadOnlyField label='Workspace name' value={workspace.name} />
            <ReadOnlyField
              label='Workspace owner'
              value={workspace.creatorUser.userName}
            />
            <StatusField
              label='Archive status'
              status={workspace.recoveryState}
            />
            <ReadOnlyField
              label='Recovered VWB Workspace ID'
              value={workspace.migratedVwbWorkspaceId || 'N/A'}
            />
          </div>
        </div>
      </ModalBody>

      <ModalFooter style={{ justifyContent: 'flex-end', gap: '0.5rem' }}>
        <Button type='secondary' onClick={onClose} disabled={startingRecovery}>
          Cancel
        </Button>
        <Button
          type='primary'
          onClick={handleRecovery}
          disabled={startingRecovery}
        >
          {recoveryButtonLabel}
        </Button>
      </ModalFooter>

      {startingRecovery && <SpinnerOverlay />}
    </Modal>
  );
};
