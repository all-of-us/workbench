import * as React from 'react';
import { useEffect, useState } from 'react';
import { Dropdown } from 'primereact/dropdown';
import {
  faCheckCircle,
  faEdit,
  faXmarkCircle,
} from '@fortawesome/free-regular-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import {
  VwbPodDescription,
  Workspace,
  WorkspaceAccessLevel,
  WorkspaceRecoveryStatus,
  WorkspaceUserAdminView,
} from 'generated/fetch';

import { Button, Clickable } from 'app/components/buttons';
import { FlexRow } from 'app/components/flex';
import {
  Modal,
  ModalBody,
  ModalFooter,
  ModalTitle,
} from 'app/components/modals';
import { SpinnerOverlay } from 'app/components/spinners';
import { rwToVwbResearchPurpose } from 'app/pages/admin/vwb/vwb-research-purpose-text';
import {
  vwbWorkspaceAdminApi,
  workspacesApi,
} from 'app/services/swagger-fetch-clients';
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

const PodField = ({
  label,
  value,
  onEdit,
}: {
  label: string;
  value?: string;
  onEdit: () => void;
}) => (
  <div>
    <span style={styles.fieldLabel}>
      {label}{' '}
      <span title='Update Recovery Pod' onClick={onEdit}>
        {' '}
        <FontAwesomeIcon icon={faEdit} style={{ cursor: 'pointer' }} />
      </span>
    </span>

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
  collaborators,
  onClose,
  reload,
}: {
  workspace: Workspace;
  collaborators: WorkspaceUserAdminView[];
  onClose: () => void;
  reload: () => void;
}) => {
  const [startingRecovery, setStartingRecovery] = useState(false);
  const [showUpdatePod, setShowUpdatePod] = useState(false);
  const [updatedPodId, setUpdatedPodId] = useState<string>(
    workspace.recoveryPodId
  );
  const [selectedPod, setSelectedPod] = useState<VwbPodDescription>();
  const [selectedUser, setSelectedUser] = useState(
    collaborators.find((c) => c.userModel.userName === workspace.creator)
      ?.userModel.userName ?? collaborators[0]?.userModel.userName
  );
  const [pods, setPods] = useState<VwbPodDescription[]>([]);
  const [loadingPods, setLoadingPods] = useState(false);

  const getPods = async (username: string) => {
    setLoadingPods(true);
    try {
      const podsResp = await vwbWorkspaceAdminApi().getPods(username);
      setPods([
        ...podsResp,
        {
          podId: 'b7a2-4ba8-9a8e-a2fb7f9b2c12-662ec7f0',
          userFacingId: 'pod-dolbeew-7f5a-user',
          description: 'Pod 2 for dolbeew@fake-research-aou.org',
        },
        {
          podId: '4ba8-9a8e-a2fb7f9b2c12-662ec7f0-b7a2',
          userFacingId: 'dolbeew-7f5a-user-pod',
          description: 'Pod 3 for dolbeew@fake-research-aou.org',
        },
      ]);
    } catch (error) {
      console.error(error);
    } finally {
      setLoadingPods(false);
    }
  };

  useEffect(() => {
    if (selectedUser) {
      getPods(selectedUser.split('@')[0]);
    }
  }, [selectedUser]);

  const handleRecovery = async () => {
    if (!workspace) {
      return;
    }

    try {
      setStartingRecovery(true);
      await workspacesApi().startWorkspaceRecovery(
        workspace.namespace,
        workspace.terraName,
        {
          researchPurpose: JSON.stringify(
            rwToVwbResearchPurpose(workspace.researchPurpose)
          ),
        }
      );

      await reload();
      onClose();
    } catch (e) {
      console.error(e);
    } finally {
      setStartingRecovery(false);
    }
  };

  const cancelPodUpdate = () => {
    setShowUpdatePod(false);
  };

  const savePodUpdate = () => {
    setShowUpdatePod(false);
    setUpdatedPodId(selectedPod.podId);
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
              label='Recovery status'
              status={workspace.recoveryState}
            />
            <PodField
              label='Recovery Pod ID'
              value={updatedPodId || 'Not set'}
              onEdit={() => setShowUpdatePod(true)}
            />
          </div>
          {showUpdatePod && (
            <>
              <span style={styles.fieldLabel}>Update Pod</span>
              <FlexRow>
                <Dropdown
                  style={{ flex: 1, marginRight: '4px' }}
                  value={selectedUser}
                  options={collaborators
                    .filter((c) => c.role === WorkspaceAccessLevel.OWNER)
                    .map((c) => c.userModel.userName)}
                  onChange={(e) => setSelectedUser(e.value)}
                  disabled={loadingPods}
                />
                <Dropdown
                  style={{ flex: 0.75, marginRight: '4px' }}
                  value={selectedPod}
                  options={pods}
                  optionLabel='userFacingId'
                  placeholder={loadingPods ? 'Loading pods...' : 'Select a pod'}
                  onChange={(e) => setSelectedPod(e.value)}
                  disabled={loadingPods}
                />
                <div style={{ flex: 0.1 }}>
                  <Clickable title='Cancel' onClick={cancelPodUpdate}>
                    {' '}
                    <FontAwesomeIcon
                      icon={faXmarkCircle}
                      style={{ color: colors.danger, cursor: 'pointer' }}
                    />
                  </Clickable>
                  <Clickable
                    title='Save Pod'
                    onClick={savePodUpdate}
                    disabled={!selectedPod}
                  >
                    {' '}
                    <FontAwesomeIcon
                      icon={faCheckCircle}
                      style={{ color: colors.success, cursor: 'pointer' }}
                    />
                  </Clickable>
                </div>
              </FlexRow>
            </>
          )}
        </div>
      </ModalBody>

      <ModalFooter style={{ justifyContent: 'flex-end', gap: '0.5rem' }}>
        <Button type='secondary' onClick={onClose} disabled={startingRecovery}>
          Cancel
        </Button>
        <Button
          type='primary'
          onClick={handleRecovery}
          disabled={startingRecovery || showUpdatePod}
        >
          {recoveryButtonLabel}
        </Button>
      </ModalFooter>

      {startingRecovery && <SpinnerOverlay />}
    </Modal>
  );
};
