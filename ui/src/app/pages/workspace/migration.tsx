import { useEffect, useState } from 'react';
import * as React from 'react';
import { Dropdown } from 'primereact/dropdown';

import { MigrationState } from 'generated/fetch';

import { environment } from 'environments/environment';
import { Button } from 'app/components/buttons';
import { ClrIcon } from 'app/components/icons';
import { TooltipTrigger } from 'app/components/popups';
import { rwToVwbResearchPurpose } from 'app/pages/admin/vwb/vwb-research-purpose-text';
import {
  disksApi,
  userApi,
  workspacesApi,
} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import { withCurrentWorkspace } from 'app/utils';
import { findCdrVersion } from 'app/utils/cdr-versions';
import { displayDate } from 'app/utils/dates';
import { currentWorkspaceStore, useNavigation } from 'app/utils/navigation';
import {
  cdrVersionStore,
  profileStore,
  serverConfigStore,
} from 'app/utils/stores';
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
  const { cdrVersionIdsForMigration } = serverConfigStore.get().config;
  const migrationTestingGroup = profile?.migrationTestingGroup ?? false;

  if (
    !migrationTestingGroup ||
    !cdrVersionIdsForMigration.includes(+workspace.cdrVersionId)
  ) {
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
      {/* TOP BANNER */}
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

      <div
        style={{
          border: `1px solid ${colors.light}`,
          borderRadius: '8px',
          padding: '16px 20px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: '16px',
        }}
      >
        <div style={{ display: 'flex', flexDirection: 'column' }}>
          <div
            style={{
              fontWeight: 600,
              fontSize: '14px',
              color: colors.primary,
            }}
          >
            {workspace.terraName}
          </div>

          <div
            style={{
              fontSize: '12px',
              color: colors.dark,
              marginTop: '4px',
            }}
          >
            {findCdrVersion(workspace.cdrVersionId, cdrVersionStore.get()).name}
          </div>
          <div
            style={{
              fontSize: '12px',
              color: colors.dark,
              marginTop: '4px',
            }}
          >
            Last Changed: {displayDate(workspace.lastModifiedTime)}
          </div>
          <div style={{ fontSize: 12 }}>
            Created By: {workspace.creatorUser.userName.split('@')[0]}
          </div>
        </div>

        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '12px',
            flex: 1,
            justifyContent: 'center',
          }}
        >
          <div style={{ display: 'flex', flexDirection: 'column' }}>
            <div
              style={{
                fontSize: '12px',
                color: colors.dark,
                fontWeight: 500,
                marginBottom: '4px',
              }}
            >
              Researcher Workbench 2.0 billing pod
            </div>

            <Dropdown
              value={selectedPod}
              options={pods}
              optionLabel='userFacingId'
              optionValue='podId'
              placeholder={loadingPods ? 'Loading pods...' : 'Select a pod'}
              onChange={(e) => setSelectedPod(e.value)}
              disabled={loadingPods}
              style={{
                width: '300px',
                borderRadius: '6px',
              }}
            />
          </div>

          <TooltipTrigger
            content={
              'You are selecting a pod for workspace storage costs in Verily Workbench. There is no cost to migrate your workspace bucket.'
            }
            side='top'
          >
            <ClrIcon
              shape='info-circle'
              size={24}
              style={{
                cursor: 'pointer',
                color: colors.accent,
                marginTop: '18px',
              }}
            />
          </TooltipTrigger>
        </div>

        <Button
          disabled={
            startingMigration ||
            !selectedPod ||
            migrationState === MigrationState.STARTING ||
            migrationState === MigrationState.FINISHED
          }
          style={{
            height: '36px',
            padding: '0 16px',
            fontSize: '13px',
            fontWeight: 600,
          }}
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

      {/* PD MODAL */}
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
