import * as React from 'react';
import { useEffect, useState } from 'react';
import fp from 'lodash/fp';
import { Button } from 'primereact/button';

import { Workspace } from 'generated/fetch';

import { WorkspaceRecoverySuccessModal } from 'app/components/migration/workspace-recovery-success-modal';
import {
  withSpinnerOverlay,
  WithSpinnerOverlayProps,
} from 'app/components/with-spinner-overlay';
import { workspacesApi } from 'app/services/swagger-fetch-clients';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import { withCurrentWorkspace } from 'app/utils';
import { reactStyles } from 'app/utils';
import { useNavigation } from 'app/utils/navigation';

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
  workspace: Workspace;
}

export const WorkspaceRecovery = fp.flow(
  withSpinnerOverlay(),
  withCurrentWorkspace()
)(({ workspace, hideSpinner }: Props) => {
  const [recovering, setRecovering] = useState(false);
  const [showSuccessModal, setShowSuccessModal] = useState(false);
  const [navigate] = useNavigation();

  useEffect(() => {
    hideSpinner();
  }, []);

  const startRecovery = async () => {
    try {
      setRecovering(true);

      await workspacesApi().requestWorkspaceRecovery(
        workspace.namespace,
        workspace.terraName
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

                <div style={styles.fieldValue}>{workspace.cdrVersionId}</div>

                <div style={styles.warning}>
                  This dataset version is no longer supported. Upon recovery,
                  the workspace will automatically be upgraded to the newest
                  supported dataset version.
                </div>
              </div>
            </div>

            <div style={styles.buttonRow}>
              <Button
                label='Cancel'
                className='p-button-outlined'
                onClick={() => navigate(['/workspaces'])}
              />
              <Button
                label='Request Workspace Recovery'
                disabled={recovering}
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
