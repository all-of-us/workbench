import * as React from 'react';
import { useEffect, useState } from 'react';
import { Dropdown } from 'primereact/dropdown';

import { WorkspaceAdminView, WorkspaceRecoveryStatus } from 'generated/fetch';

import { Button } from 'app/components/buttons';
import { TextInputWithLabel } from 'app/components/inputs';
import {
  Modal,
  ModalBody,
  ModalFooter,
  ModalTitle,
} from 'app/components/modals';
import { SpinnerOverlay } from 'app/components/spinners';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import {
  vwbWorkspaceAdminApi,
  workspaceAdminApi,
  workspacesApi,
} from 'app/services/swagger-fetch-clients';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import { reactStyles } from 'app/utils';

const styles = reactStyles({
  page: {
    margin: '1.5rem',
  },

  pageHeader: {
    marginBottom: '1.5rem',
  },

  searchRow: {
    display: 'flex',
    alignItems: 'flex-end',
    gap: '1rem',
    marginBottom: '1.5rem',
  },

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

export const AdminWorkspaceArchivalRecovery = (
  spinnerProps: WithSpinnerOverlayProps
) => {
  const [workspaceNamespace, setWorkspaceNamespace] = useState('');
  const [workspace, setWorkspace] = useState<WorkspaceAdminView>(null);
  const [loading, setLoading] = useState(false);
  const [fetchError, setFetchError] = useState(false);
  const [pods, setPods] = useState<string[]>([]);
  const [selectedPod, setSelectedPod] = useState('');
  const [loadingPods, setLoadingPods] = useState(false);
  const [startingRecovery, setStartingRecovery] = useState(false);
  const [recoveryState, setRecoveryState] = useState<WorkspaceRecoveryStatus>(
    WorkspaceRecoveryStatus.NOT_STARTED
  );

  useEffect(() => spinnerProps.hideSpinner(), []);

  const findWorkspace = async () => {
    try {
      setFetchError(false);
      setLoading(true);
      const response = await workspaceAdminApi().getWorkspaceAdminView(
        workspaceNamespace
      );
      setWorkspace(response);
    } catch (e) {
      console.error(e);
      setFetchError(true);
    } finally {
      setLoading(false);
    }
  };

  const getPods = async () => {
    if (!workspace) {
      return;
    }

    try {
      setLoadingPods(true);

      const username = workspace.workspace.creatorUser.userName;

      const response = await vwbWorkspaceAdminApi().getPods(username);

      setPods([
        'nph-consortium-users',
        ...(response || []),
        'aou-system-billing-prod',
      ]);
    } catch (e) {
      console.error(e);
    } finally {
      setLoadingPods(false);
    }
  };

  const clearForm = () => {
    setWorkspace(null);
    setSelectedPod('');
    setRecoveryState(WorkspaceRecoveryStatus.NOT_STARTED);
  };

  const handleRecovery = async () => {
    if (!workspace || !selectedPod) {
      return;
    }

    try {
      setStartingRecovery(true);

      await workspacesApi().startWorkspaceRecovery(
        workspace.workspace.namespace,
        selectedPod
      );

      await findWorkspace();
    } catch (e) {
      console.error(e);
    } finally {
      setStartingRecovery(false);
    }
  };

  const recoveryButtonLabel = startingRecovery
    ? 'Starting...'
    : recoveryState === WorkspaceRecoveryStatus.RECOVERING
    ? 'Recovery in progress'
    : 'Start recovery';

  return (
    <div style={styles.page}>
      <h2 style={styles.pageHeader}>Workspace archive recovery</h2>

      <div style={styles.searchRow}>
        <TextInputWithLabel
          style={{ width: '22.5rem' }}
          labelText='Workspace namespace'
          value={workspaceNamespace}
          onChange={setWorkspaceNamespace}
          onKeyDown={(event: KeyboardEvent) => {
            if (!!workspaceNamespace && event.key === 'Enter') {
              void findWorkspace();
            }
          }}
        />
        <Button
          style={{ height: '2.25rem', marginTop: '1.4rem' }}
          disabled={!workspaceNamespace}
          onClick={findWorkspace}
        >
          Find workspace
        </Button>
      </div>

      {fetchError && (
        <p
          style={{
            color: colors.danger,
            fontSize: '13px',
            marginTop: '0.75rem',
          }}
        >
          Error loading workspace. Please try again.
        </p>
      )}

      {loading && <SpinnerOverlay />}

      {workspace && (
        <Modal
          data-test-id='workspace-archive-recovery-modal'
          aria={{ label: 'Workspace Archive Recovery' }}
        >
          <ModalTitle>Workspace archive recovery</ModalTitle>

          <ModalBody>
            <div style={styles.modalBody}>
              {/* Info grid */}
              <div style={styles.grid}>
                <ReadOnlyField
                  label='Workspace name'
                  value={workspace.workspace.name}
                />
                <ReadOnlyField
                  label='Workspace owner'
                  value={workspace.workspace.creatorUser.userName}
                />
                <StatusField
                  label='Archive status'
                  status={workspace.workspace.recoveryState}
                />
                <ReadOnlyField
                  label='Recovered VWB workspace ID'
                  value={workspace.workspace.migratedVwbWorkspaceId || 'N/A'}
                />
              </div>

              <hr style={styles.divider} />

              {/* Pod selection */}
              <div style={styles.podSection}>
                <span style={styles.sectionLabel}>
                  Researcher Workbench 2.0 billing pod
                </span>
                <div style={styles.podRow}>
                  <Dropdown
                    value={selectedPod}
                    options={pods}
                    placeholder={
                      loadingPods ? 'Loading pods...' : 'Select a billing pod'
                    }
                    onChange={(e) => setSelectedPod(e.value)}
                    disabled={loadingPods}
                    style={{
                      flex: 1,
                      borderRadius: '6px',
                    }}
                  />
                  <Button
                    type='secondarySmall'
                    onClick={getPods}
                    disabled={loadingPods || startingRecovery}
                  >
                    {loadingPods ? 'Loading...' : 'Load pods'}
                  </Button>
                </div>
              </div>
            </div>
          </ModalBody>

          <ModalFooter style={{ justifyContent: 'flex-end', gap: '0.5rem' }}>
            <Button
              type='secondary'
              onClick={clearForm}
              disabled={startingRecovery}
            >
              Cancel
            </Button>
            <Button
              type='primary'
              onClick={handleRecovery}
              disabled={startingRecovery || !selectedPod}
            >
              {recoveryButtonLabel}
            </Button>
          </ModalFooter>
        </Modal>
      )}
    </div>
  );
};
