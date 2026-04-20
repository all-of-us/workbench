import { useEffect, useState } from 'react';

import { MigrationState } from 'generated/fetch';

import { environment } from 'environments/environment';
import { Button } from 'app/components/buttons';
import { rwToVwbResearchPurpose } from 'app/pages/admin/vwb/vwb-research-purpose-text';
import {
  disksApi,
  userApi,
  workspacesApi,
} from 'app/services/swagger-fetch-clients';
import { withCurrentWorkspace } from 'app/utils';
import { currentWorkspaceStore, useNavigation } from 'app/utils/navigation';
import { profileStore } from 'app/utils/stores';
import { WorkspaceData } from 'app/utils/workspace-data';

import { PdWarningModal } from './pd-warning-modal';
import { VwbImportantBanner } from './vwb-important-banner';
import { VwbMigrationInfoBox } from './vwb-migration-infobox';

const WORKSPACE_MIGRATION_POLL_INTERVAL_MS = 5 * 1000;

interface Props {
  workspace: WorkspaceData;
}

export const MigrationPage = withCurrentWorkspace()(({ workspace }: Props) => {
  const [navigate] = useNavigation();
  const [selectedPod, setSelectedPod] = useState('');
  const [pods, setPods] = useState<any[]>([]);
  const [loadingPods, setLoadingPods] = useState(false);
  const [startingMigration, setStartingMigration] = useState(false);
  const [hasAcceptedTos, setHasAcceptedTos] = useState<boolean | null>(null);
  const [hasPersistentDisk, setHasPersistentDisk] = useState<boolean | null>(
    null
  );
  const [showPdModal, setShowPdModal] = useState(false);

  const [migrationState, setMigrationState] = useState<MigrationState>(
    workspace?.migrationState ?? MigrationState.NOT_STARTED
  );
  const [transferTimeoutId, setTransferTimeoutId] =
    useState<NodeJS.Timeout>(undefined);

  if (!workspace) {
    return null;
  }

  const profile = profileStore.get().profile;
  const migrationTestingGroup = profile?.migrationTestingGroup ?? false;

  if (!migrationTestingGroup) {
    navigate(['workspaces', workspace.namespace, workspace.terraName, 'data']);
  }

  const checkWorkspaceMigrationStatus = async () => {
    const workspaceStatusCheck = await workspacesApi().getWorkspace(
      workspace.namespace,
      workspace.terraName
    );
    if (
      workspaceStatusCheck.workspace.migrationState === MigrationState.STARTING
    ) {
      if (!transferTimeoutId) {
        const timeoutId = setTimeout(
          checkWorkspaceMigrationStatus,
          WORKSPACE_MIGRATION_POLL_INTERVAL_MS
        );
        setTransferTimeoutId(timeoutId);
      }
    }
    setMigrationState(workspaceStatusCheck.workspace.migrationState);
  };

  useEffect(() => {
    if (workspace?.migrationState) {
      setMigrationState(workspace.migrationState);
    }
  }, [workspace.migrationState]);

  // Load Pods
  useEffect(() => {}, []);

  // Start Migration
  const handleMigration = async () => {
    try {
      setStartingMigration(true);
      currentWorkspaceStore.next({
        ...workspace,
        migrationState: MigrationState.STARTING,
      });

      await workspacesApi().startWorkspaceMigration(
        workspace.namespace,
        workspace.terraName,
        {
          folders: [],
          podId: selectedPod,
          researchPurpose: JSON.stringify(
            rwToVwbResearchPurpose(workspace.researchPurpose)
          ),
        }
      );
      void checkWorkspaceMigrationStatus();
      setMigrationState(MigrationState.STARTING);
    } catch (e) {
      console.error('Migration failed', e);

      setMigrationState(MigrationState.FAILED);
    } finally {
      setStartingMigration(false);
    }
  };

  useEffect(() => {
    if (
      workspace.migrationState === MigrationState.STARTING &&
      !startingMigration
    ) {
      void checkWorkspaceMigrationStatus();
    }
    const loadPods = async () => {
      setLoadingPods(true);
      try {
        const response = await workspacesApi().getUserPods();
        setPods(response || []);
      } catch (e) {
        console.error('Failed to load pods', e);
      } finally {
        setLoadingPods(false);
      }
    };
    const fetchTos = async () => {
      try {
        const res = await userApi().getUserTosStatus();
        setHasAcceptedTos(res);
      } catch (e) {
        console.error('Failed to fetch ToS state', e);
        setHasAcceptedTos(false);
      }
    };

    loadPods();
    fetchTos();
  }, []);

  useEffect(() => {
    const checkDisks = async () => {
      try {
        const disks = await disksApi().listOwnedDisksInWorkspace(
          workspace.namespace
        );

        setHasPersistentDisk(disks && disks.length > 0);
      } catch (e) {
        console.error('Failed to check disks', e);
        setHasPersistentDisk(false);
      }
    };

    checkDisks();
  }, [workspace.namespace]);

  const handleStartClick = () => {
    if (hasPersistentDisk) {
      setShowPdModal(true);
    } else {
      void handleMigration();
    }
  };

  return (
    <div style={{ padding: '1.5rem 2rem' }}>
      {/*  TOP BANNER */}
      {!hasAcceptedTos && (
        <VwbImportantBanner
          title='Important'
          message={`Before starting migration, log into Researcher Workbench 2.0 
to agree to the terms of service. You only need to do this once.`}
          actionText='Open Verily Workbench'
          onAction={() => window.open(environment.vwbUiUrl, '_blank')}
          onClose={() => setHasAcceptedTos(true)}
        />
      )}

      <VwbMigrationInfoBox />

      {/*  WORKSPACE ROW */}
      <div
        style={{
          border: '1px solid #D3DAE6',
          borderRadius: '8px',
          padding: '16px',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
        }}
      >
        {/* Left Info */}
        <div>
          <div style={{ fontWeight: 600 }}>{workspace.terraName}</div>
          <div style={{ fontSize: '13px', color: '#6B7280' }}>
            Controlled Tier Dataset
          </div>
        </div>

        {/* Pod Select */}
        <div style={{ width: '300px' }}>
          <select
            value={selectedPod}
            onChange={(e) => setSelectedPod(e.target.value)}
            style={{
              width: '100%',
              padding: '8px',
              borderRadius: '6px',
              border: '1px solid #D3DAE6',
            }}
            disabled={loadingPods}
          >
            <option value=''>
              {loadingPods ? 'Loading pods...' : 'Select a pod'}
            </option>
            {pods.map((pod) => (
              <option key={pod.podId} value={pod.podId}>
                {pod.userFacingId || pod.description || pod.podId}
              </option>
            ))}
          </select>
        </div>

        {/* CTA */}
        <Button
          disabled={
            startingMigration ||
            !selectedPod ||
            migrationState === MigrationState.STARTING ||
            migrationState === MigrationState.FINISHED
          }
          onClick={handleStartClick}
        >
          {startingMigration
            ? 'Starting...'
            : migrationState === MigrationState.STARTING
            ? 'Migration in progress'
            : migrationState === MigrationState.FINISHED
            ? 'Migrated'
            : migrationState === MigrationState.FAILED
            ? 'Retry migration'
            : 'Start migration'}
        </Button>
      </div>

      {showPdModal && (
        <PdWarningModal
          onCancel={() => setShowPdModal(false)}
          onConfirm={() => {
            setShowPdModal(false);
            void handleMigration();
          }}
        />
      )}
    </div>
  );
});
