import { useEffect, useState } from 'react';
import * as React from 'react';

import { MigrationState } from 'generated/fetch';

import { environment } from 'environments/environment';
import { Button } from 'app/components/buttons';
import { CheckBox } from 'app/components/inputs';
import { Spinner } from 'app/components/spinners';
import { rwToVwbResearchPurpose } from 'app/pages/admin/vwb/vwb-research-purpose-text';
import {
  disksApi,
  userApi,
  workspacesApi,
} from 'app/services/swagger-fetch-clients';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import { withCurrentWorkspace } from 'app/utils';
import { findCdrVersion } from 'app/utils/cdr-versions';
import { displayDate } from 'app/utils/dates';
import { useNavigation } from 'app/utils/navigation';
import {
  cdrVersionStore,
  profileStore,
  serverConfigStore,
} from 'app/utils/stores';
import { WorkspaceData } from 'app/utils/workspace-data';

import { PdWarningModal } from './pd-warning-modal';
import { VwbImportantBanner } from './vwb-important-banner';
import { VwbMigrationSyncInfoBox } from './vwb-migration-sync-infobox';

const WORKSPACE_MIGRATION_POLL_INTERVAL_MS = 5 * 1000;

interface Props {
  workspace: WorkspaceData;
}

export const MigrationFolderSync = withCurrentWorkspace()(
  ({ workspace }: Props) => {
    const [navigate] = useNavigation();
    const [startingFolderSync, setStartingFolderSync] = useState(false);
    const [loadingTos, setLoadingTos] = useState(true);
    const [hasAcceptedTos, setHasAcceptedTos] = useState<boolean | null>(null);
    const [hasPersistentDisk, setHasPersistentDisk] = useState<boolean | null>(
      null
    );
    const [showPdModal, setShowPdModal] = useState(false);
    const [folders, setFolders] = useState<string[]>([]);
    const [selectedFolders, setSelectedFolders] = useState<string[]>([]);
    const [selectAll, setSelectAll] = useState(false);
    const [loadingFolders, setLoadingFolders] = useState(true);

    const [migrationState, setMigrationState] = useState<MigrationState>(
      workspace?.migrationState ?? MigrationState.NOT_STARTED
    );
    const [transferTimeoutId, setTransferTimeoutId] =
      useState<NodeJS.Timeout>(undefined);

    if (!workspace) {
      return null;
    }

    const profile = profileStore.get().profile;
    const { cdrVersionsForMigration } = serverConfigStore.get().config;
    const migrationTestingGroup = profile?.migrationTestingGroup ?? false;

    if (
      !migrationTestingGroup ||
      !cdrVersionsForMigration.some(
        (c) => +workspace.cdrVersionId === c.cdrVersionId
      )
    ) {
      navigate([
        'workspaces',
        workspace.namespace,
        workspace.terraName,
        'data',
      ]);
    }

    const checkFolderSyncStatus = async () => {
      const workspaceStatusCheck = await workspacesApi().getWorkspace(
        workspace.namespace,
        workspace.terraName
      );
      if (
        workspaceStatusCheck.workspace.migrationState ===
        MigrationState.STARTING
      ) {
        if (!transferTimeoutId) {
          const timeoutId = setTimeout(
            checkFolderSyncStatus,
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

    // Start Folder Sync
    const handleFolderSync = async () => {
      try {
        setStartingFolderSync(true);

        await workspacesApi().startWorkspaceMigration(
          workspace.namespace,
          workspace.terraName,
          {
            folders: selectAll ? [] : selectedFolders,
            podId: 'selectedPod',
            researchPurpose: JSON.stringify(
              rwToVwbResearchPurpose(workspace.researchPurpose)
            ),
          }
        );
        void checkFolderSyncStatus();
        setMigrationState(MigrationState.STARTING);
      } catch (e) {
        console.error('Migration failed', e);

        setMigrationState(MigrationState.FAILED);
      } finally {
        setStartingFolderSync(false);
      }
    };

    useEffect(() => {
      if (
        workspace.migrationState === MigrationState.STARTING &&
        !startingFolderSync
      ) {
        void checkFolderSyncStatus();
      }
      const fetchBucketContents = async () => {
        try {
          const res = await workspacesApi().getMigrationBucketContents(
            workspace.namespace,
            workspace.terraName
          );
          setFolders(res.folders);
        } catch (e) {
          console.error('Failed to fetch ToS state', e);
        } finally {
          setLoadingFolders(false);
        }
      };
      const fetchTos = async () => {
        try {
          const res = await userApi().getUserTosStatus();
          setHasAcceptedTos(res);
        } catch (e) {
          console.error('Failed to fetch ToS state', e);
          setHasAcceptedTos(false);
        } finally {
          setLoadingTos(false);
        }
      };
      void fetchBucketContents();
      void fetchTos();
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
        void handleFolderSync();
      }
    };

    return (
      <div style={{ padding: '1.5rem 2rem' }}>
        {/* TOP BANNER */}
        {!hasAcceptedTos && (
          <VwbImportantBanner
            title='Important'
            message={`Before syncing folders, log into Researcher Workbench 2.0 
to agree to the terms of service. You only need to do this once.`}
            actionText='Open Researcher Workbench 2.0'
            onAction={() => window.open(environment.vwbUiUrl, '_blank')}
            onClose={() => setHasAcceptedTos(true)}
            loadingText={loadingTos && 'Checking Terms of Service'}
          />
        )}

        <VwbMigrationSyncInfoBox />

        <div
          style={{
            background: colors.white,
            border: `1px solid ${colorWithWhiteness(colors.dark, 0.7)}`,
            borderRadius: '8px',
            padding: '16px 20px',
          }}
        >
          <div
            style={{
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
                {
                  findCdrVersion(workspace.cdrVersionId, cdrVersionStore.get())
                    .name
                }
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

            <Button
              disabled={
                migrationState === MigrationState.STARTING ||
                selectedFolders.length === 0
              }
              style={{
                height: '36px',
                padding: '0 16px',
                fontSize: '13px',
                fontWeight: 600,
              }}
              onClick={handleStartClick}
            >
              {startingFolderSync
                ? 'Sync in progress'
                : 'Sync to Researcher Workbench 2.0'}
            </Button>
          </div>
          {loadingFolders ? (
            <div style={{ textAlign: 'center' }}>
              <Spinner />
            </div>
          ) : (
            <>
              {/* Select All Checkbox */}
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '16px',
                  padding: '16px 0',
                  borderBottom: `1px solid ${colorWithWhiteness(
                    colors.dark,
                    0.7
                  )}`,
                }}
              >
                <CheckBox
                  manageOwnState={false}
                  checked={
                    selectAll || folders.length === selectedFolders.length
                  }
                  onChange={(checked) => {
                    setSelectAll(checked);
                    setSelectedFolders(checked ? folders : []);
                  }}
                />
                <div
                  style={{
                    fontWeight: 600,
                    fontSize: '14px',
                    color: colors.black,
                  }}
                >
                  Select All
                </div>
              </div>
              {folders.map((folder, index) => (
                <div
                  key={index}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: '16px',
                    padding: '16px 0',
                    borderBottom: `1px solid ${colorWithWhiteness(
                      colors.dark,
                      0.7
                    )}`,
                  }}
                >
                  <CheckBox
                    manageOwnState={false}
                    checked={selectedFolders.includes(folder)}
                    onChange={(checked) => {
                      if (!checked) {
                        setSelectAll(false);
                      }
                      setSelectedFolders((prevState) =>
                        checked
                          ? [...prevState, folder]
                          : prevState.filter((f) => f !== folder)
                      );
                    }}
                  />
                  <div
                    style={{
                      fontWeight: 600,
                      fontSize: '14px',
                      color: colors.black,
                    }}
                  >
                    {folder.slice(0, -1)}
                  </div>
                </div>
              ))}
            </>
          )}
        </div>

        {/* PD MODAL */}
        {showPdModal && (
          <PdWarningModal
            onCancel={() => setShowPdModal(false)}
            onConfirm={() => {
              setShowPdModal(false);
              void handleFolderSync();
            }}
          />
        )}
      </div>
    );
  }
);
