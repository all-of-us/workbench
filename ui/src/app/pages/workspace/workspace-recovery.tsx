import * as React from 'react';
import { useEffect, useState } from 'react';
import fp from 'lodash/fp';
import { Button } from 'primereact/button';
import { Dropdown } from 'primereact/dropdown';

import { WorkspaceAccessLevel, WorkspaceRecoveryStatus } from 'generated/fetch';

import { environment } from 'environments/environment';
import { ClrIcon } from 'app/components/icons';
import { WorkspaceRecoverySuccessModal } from 'app/components/migration/workspace-recovery-success-modal';
import { TooltipTrigger } from 'app/components/popups';
import {
  withSpinnerOverlay,
  WithSpinnerOverlayProps,
} from 'app/components/with-spinner-overlay';
import { userApi, workspacesApi } from 'app/services/swagger-fetch-clients';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import { reactStyles, withCurrentWorkspace } from 'app/utils';
import { findCdrVersion } from 'app/utils/cdr-versions';
import { useNavigation } from 'app/utils/navigation';
import { cdrVersionStore, serverConfigStore } from 'app/utils/stores';
import { WorkspaceData } from 'app/utils/workspace-data';

import { VwbImportantBanner } from './vwb-important-banner';

const styles = reactStyles({
  page: {
    maxWidth: '1350px',
    margin: '48px auto',
    padding: '0 32px',
  },

  cards: {
    display: 'flex',
    gap: '28px',
    alignItems: 'flex-start',
  },

  leftCard: {
    width: '360px',
    background: colors.white,
    border: `1px solid ${colorWithWhiteness(colors.dark, 0.88)}`,
    borderRadius: 6,
    padding: '28px',
    boxShadow: '0 1px 4px rgba(0,0,0,.08)',
  },

  rightCard: {
    flex: 1,
    background: colors.white,
    border: `1px solid ${colorWithWhiteness(colors.dark, 0.88)}`,
    borderRadius: 6,
    padding: '28px 34px',
    boxShadow: '0 1px 4px rgba(0,0,0,.08)',
  },

  cardTitle: {
    fontSize: 30,
    fontWeight: 600,
    color: colors.primary,
    marginBottom: 18,
  },

  divider: {
    borderBottom: `1px solid ${colorWithWhiteness(colors.dark, 0.88)}`,
    marginBottom: 22,
  },

  sectionTitle: {
    fontWeight: 600,
    color: colors.primary,
    fontSize: 18,
    marginBottom: 12,
  },

  paragraph: {
    fontSize: 14,
    color: colors.dark,
    lineHeight: '24px',
    marginBottom: 12,
  },

  orderedList: {
    paddingLeft: 20,
    lineHeight: '28px',
    color: colors.dark,
    fontSize: 14,
    marginBottom: 24,
  },

  unorderedList: {
    paddingLeft: 18,
    lineHeight: '24px',
    color: colors.dark,
    fontSize: 14,
  },

  fieldLabel: {
    fontWeight: 600,
    color: colors.primary,
    fontSize: 15,
    marginBottom: 6,
  },

  fieldValue: {
    fontSize: 15,
    color: colors.dark,
    marginBottom: 28,
  },

  grid: {
    display: 'grid',
    gridTemplateColumns: '1fr 1fr',
    gap: '32px',
    marginBottom: 24,
  },

  warning: {
    background: '#FCE5C8',
    borderRadius: 4,
    padding: '14px',
    fontSize: 13,
    color: colors.dark,
    marginTop: 12,
    lineHeight: '20px',
  },

  select: {
    width: '100%',
    height: 42,
    borderRadius: 4,
    border: `1px solid ${colorWithWhiteness(colors.dark, 0.75)}`,
    padding: '0 12px',
    fontSize: 14,
  },

  buttonRow: {
    display: 'flex',
    justifyContent: 'flex-end',
    gap: '12px',
    marginTop: 36,
  },
});
interface Props extends WithSpinnerOverlayProps {
  workspace: WorkspaceData;
}

export const WorkspaceRecovery = fp.flow(
  withSpinnerOverlay(),
  withCurrentWorkspace()
)(({ workspace, hideSpinner }: Props) => {
  const [recovering, setRecovering] = useState(false);
  const [selectedPod, setSelectedPod] = useState('');
  const [pods, setPods] = useState<any[]>([]);
  const [loadingPods, setLoadingPods] = useState(false);
  const [loadingTos, setLoadingTos] = useState(true);
  const [hasAcceptedTos, setHasAcceptedTos] = useState<boolean | null>(null);
  const [showSuccessModal, setShowSuccessModal] = useState(false);
  const [navigate] = useNavigation();
  const { cdrVersionsForMigration } = serverConfigStore.get().config;

  if (workspace.accessLevel !== WorkspaceAccessLevel.OWNER) {
    navigate(['workspaces']);
  }

  useEffect(() => {
    hideSpinner();
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
      } finally {
        setLoadingTos(false);
      }
    };

    loadPods();
    fetchTos();
  }, []);

  const startRecovery = async () => {
    try {
      setRecovering(true);

      await workspacesApi().requestWorkspaceRecovery(
        workspace.namespace,
        workspace.terraName,
        selectedPod
      );
      setShowSuccessModal(true);
    } catch (e) {
      console.error('Failed to request workspace recovery', e);
    } finally {
      setRecovering(false);
    }
  };

  return (
    <>
      <div style={styles.page}>
        {!hasAcceptedTos && (
          <VwbImportantBanner
            title='Important'
            message={`Before starting migration, log into Researcher Workbench 2.0 
to agree to the terms of service. You only need to do this once.`}
            actionText='Open Researcher Workbench 2.0'
            onAction={() => window.open(environment.vwbUiUrl, '_blank')}
            onClose={() => setHasAcceptedTos(true)}
            loadingText={loadingTos && 'Checking Terms of Service'}
          />
        )}
        <div style={styles.cards}>
          {/* LEFT CARD */}

          <div style={styles.leftCard}>
            <div style={styles.cardTitle}>Recovering Your Workspace</div>

            <div style={styles.divider} />

            <div style={styles.sectionTitle}>How It Works</div>

            <div style={styles.paragraph}>
              To recover this workspace, here's what you need to do:
            </div>

            <ol style={styles.orderedList}>
              <li>
                Review the workspace information displayed to confirm this is
                the workspace you want to recover.
              </li>

              <li>
                Select <b>Request Workspace Recovery</b> to submit your recovery
                request.
              </li>

              <li>
                Our support team will review your request and initiate the
                recovery process.
              </li>
            </ol>

            <div style={styles.sectionTitle}>What Happens Next</div>

            <ul style={styles.unorderedList}>
              <li>
                If your workspace has an older CDR version (lower than CDRv7),
                we'll automatically upgrade it to the newest supported version.
              </li>

              <li>
                You will not lose any notebooks or analysis associated with this
                workspace.
              </li>

              <li>
                We'll notify you once your workspace has been restored and is
                ready to use.
              </li>

              <li>
                Once recovered, normal storage charges for the workspace will
                resume.
              </li>
            </ul>
          </div>

          {/* RIGHT CARD */}

          <div style={styles.rightCard}>
            <div style={styles.cardTitle}>Recover Workspace From Storage</div>

            <div style={styles.paragraph}>
              This workspace and its data are currently archived in storage.
              Verify the information below and then request recovery. Our
              support team will handle the recovery process.
            </div>

            <div style={styles.divider} />

            <div style={styles.fieldLabel}>Workspace Name</div>
            <div style={styles.fieldValue}>{workspace.name}</div>

            <div style={styles.grid}>
              <div>
                <div style={styles.fieldLabel}>Workspace ID</div>
                <div style={styles.fieldValue}>{workspace.namespace}</div>
              </div>

              <div>
                <div style={styles.fieldLabel}>Workspace Creator</div>
                <div style={styles.fieldValue}>
                  {workspace.creatorUser?.userName}
                </div>
              </div>
            </div>

            <div style={styles.grid}>
              <div>
                <div style={styles.fieldLabel}>Workspace Dataset Version</div>

                <div style={styles.fieldValue}>
                  {
                    findCdrVersion(
                      workspace.cdrVersionId,
                      cdrVersionStore.get()
                    ).name
                  }
                </div>

                {!cdrVersionsForMigration.some(
                  (c) => +workspace.cdrVersionId === c.cdrVersionId
                ) && (
                  <div style={styles.warning}>
                    This dataset version is no longer supported. Upon recovery,
                    the workspace will automatically be upgraded to the newest
                    supported dataset version.
                  </div>
                )}
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
                  <div style={styles.fieldLabel}>
                    Researcher Workbench 2.0 billing pod
                  </div>

                  <Dropdown
                    value={selectedPod}
                    options={pods}
                    optionLabel='userFacingId'
                    optionValue='podId'
                    placeholder={
                      loadingPods ? 'Loading pods...' : 'Select a pod'
                    }
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
                    'You are selecting a pod for workspace storage costs in Verily Workbench. There is no cost to recover your workspace.'
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
            </div>

            <div style={styles.buttonRow}>
              <Button
                label='Cancel'
                className='p-button-outlined'
                onClick={() => navigate(['/workspaces'])}
              />
              <Button
                label={
                  workspace.recoveryState === WorkspaceRecoveryStatus.REQUESTED
                    ? 'Recovery Request Submitted'
                    : 'Request Workspace Recovery'
                }
                disabled={
                  recovering ||
                  workspace.recoveryState ===
                    WorkspaceRecoveryStatus.REQUESTED ||
                  !hasAcceptedTos ||
                  !selectedPod
                }
                loading={recovering}
                onClick={startRecovery}
              />
            </div>
          </div>
        </div>
      </div>
      {showSuccessModal && (
        <WorkspaceRecoverySuccessModal
          onClose={() => setShowSuccessModal(false)}
          onReturn={() => navigate(['/workspaces'])}
        />
      )}
    </>
  );
});
