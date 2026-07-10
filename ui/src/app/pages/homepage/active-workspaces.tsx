import * as React from 'react';
import { faUpRightFromSquare } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { WorkspaceResponse } from 'generated/fetch';

import { environment } from 'environments/environment';
import { StyledExternalLink, StyledRouterLink } from 'app/components/buttons';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { ClrIcon } from 'app/components/icons';
import colors, { addOpacity } from 'app/styles/colors';
import { reactStyles } from 'app/utils';
import { badgeForTier, displayNameForTier } from 'app/utils/access-tiers';

interface Props {
  workspaces: WorkspaceResponse[];
}

const styles = reactStyles({
  headerRow: {
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: '0.6rem',
  },
  title: {
    color: colors.primary,
    fontSize: '1.3rem',
    fontWeight: 600,
    margin: 0,
  },
  archivedLink: {
    alignItems: 'center',
    color: colors.primary,
    display: 'flex',
    fontSize: '0.9rem',
    gap: '0.4rem',
    textDecoration: 'underline',
  },
  rowsContainer: {
    borderTop: `1px solid ${addOpacity(colors.dark, 0.2).toString()}`,
  },
  workspaceRow: {
    borderBottom: `1px solid ${addOpacity(colors.dark, 0.18).toString()}`,
    padding: '0.7rem 0',
  },
  workspaceNameLink: {
    color: colors.dark,
    fontSize: '0.95rem',
    fontWeight: 600,
    marginRight: '0.75rem',
    textDecoration: 'none',
  },
  detailsText: {
    color: addOpacity(colors.dark, 0.8).toString(),
    display: '-webkit-box',
    fontSize: '0.8rem',
    lineHeight: 1.4,
    marginTop: '0.35rem',
    overflow: 'hidden',
    WebkitBoxOrient: 'vertical',
    WebkitLineClamp: 2,
  },
  accessBadge: {
    borderRadius: '0.4rem',
    color: colors.white,
    fontSize: '0.7rem',
    fontWeight: 600,
    lineHeight: 1,
    padding: '0.25rem 0.45rem',
  },
  accessTierFallbackBadge: {
    backgroundColor: addOpacity(colors.warning, 0.18).toString(),
    border: `1px solid ${addOpacity(colors.warning, 0.65).toString()}`,
    borderRadius: '999px',
    color: colors.warningAlt,
    fontSize: '0.95rem',
    fontWeight: 600,
    lineHeight: 1,
    padding: '0.2rem 0.45rem',
    textTransform: 'uppercase',
  },
  footerLink: {
    color: colors.accent,
    display: 'inline-flex',
    fontSize: '1rem',
    marginTop: '1rem',
    textDecoration: 'underline',
  },
  openButton: {
    backgroundColor: colors.success,
    borderRadius: '2rem',
    color: colors.white,
    display: 'inline-flex',
    fontSize: '0.95rem',
    fontWeight: 600,
    marginTop: '1rem',
    minWidth: '17rem',
    padding: '0.6rem 1.3rem',
    textTransform: 'uppercase',
  },
  emptyStateText: {
    color: addOpacity(colors.dark, 0.85).toString(),
    fontSize: '0.85rem',
    lineHeight: 1.4,
    margin: '0.75rem 0 0.2rem',
  },
  panel: {
    backgroundColor: colors.white,
    border: `1px solid ${addOpacity(colors.dark, 0.12).toString()}`,
    borderRadius: '0.35rem',
    boxShadow: `0 1px 2px ${addOpacity(colors.dark, 0.08).toString()}`,
    padding: '1.1rem 1.3rem',
  },
});

const archivedRecoveryStates = new Set([
  'NOT_STARTED',
  'REQUESTED',
  'RECOVERING',
  'FAILED',
]);

const isArchivedWorkspace = (workspaceResponse: WorkspaceResponse) =>
  archivedRecoveryStates.has(workspaceResponse.workspace.recoveryState);

const getWorkspaceDescription = (workspaceResponse: WorkspaceResponse) =>
  workspaceResponse.workspace.researchPurpose?.intendedStudy ||
  'No workspace summary available yet.';

const getAccessTierBadge = (shortName?: string) => {
  if (!shortName) {
    return null;
  }
  const badge = badgeForTier(shortName);
  if (badge) {
    return (
      <div aria-label={`${displayNameForTier(shortName)} badge`}>{badge}</div>
    );
  }
  return <div style={styles.accessTierFallbackBadge}>{shortName}</div>;
};

export const ActiveWorkspaces = ({ workspaces }: Props) => {
  const archivedCount = workspaces.filter(isArchivedWorkspace).length;
  const activeWorkspaces = workspaces
    .filter(
      (workspaceResponse) =>
        !isArchivedWorkspace(workspaceResponse) &&
        workspaceResponse.workspace.migrationState === 'FINISHED'
    )
    .sort(
      (a, b) =>
        (b.workspace.lastModifiedTime || 0) -
        (a.workspace.lastModifiedTime || 0)
    )
    .slice(0, 3);

  return (
    <div style={styles.panel} data-test-id='active-workspaces-panel'>
      <FlexRow style={styles.headerRow}>
        <h2 style={styles.title}>Active Workspaces</h2>
        <StyledRouterLink
          path='/workspaces'
          style={styles.archivedLink}
          aria-label={`View ${archivedCount} archived workspace${
            archivedCount === 1 ? '' : 's'
          }`}
        >
          <ClrIcon shape='archive' size={16} />
          {archivedCount === 1
            ? 'You have 1 archived workspace'
            : `You have ${archivedCount} archived workspaces`}
        </StyledRouterLink>
      </FlexRow>

      {activeWorkspaces.length > 0 ? (
        <div style={styles.rowsContainer}>
          {activeWorkspaces.map((workspaceResponse) => {
            const { workspace, accessLevel } = workspaceResponse;
            const workspaceName = workspace.displayName || workspace.name;
            return (
              <FlexColumn
                key={`${workspace.namespace}-${workspace.terraName}`}
                style={styles.workspaceRow}
              >
                <FlexRow style={{ alignItems: 'center', gap: '0.6rem' }}>
                  <StyledRouterLink
                    path={`/workspaces/${workspace.namespace}/${workspace.terraName}/data`}
                    style={styles.workspaceNameLink}
                    aria-label={`Open ${workspaceName} workspace`}
                  >
                    {workspaceName}
                  </StyledRouterLink>
                  <div
                    data-test-id='workspace-access-level'
                    style={{
                      ...styles.accessBadge,
                      backgroundColor:
                        colors.workspacePermissionsHighlights[accessLevel] ||
                        colors.dark,
                    }}
                  >
                    {accessLevel}
                  </div>
                  {getAccessTierBadge(workspace.accessTierShortName)}
                </FlexRow>
                <div style={styles.detailsText}>
                  {getWorkspaceDescription(workspaceResponse)}
                </div>
              </FlexColumn>
            );
          })}
        </div>
      ) : (
        <p style={styles.emptyStateText}>
          You do not have active legacy workspaces yet. Visit your workspace
          list to recover archived workspaces or create a new one.
        </p>
      )}

      <StyledExternalLink
        href={environment.vwbUiUrl}
        target='_blank'
        rel='noopener noreferrer'
        style={styles.openButton}
        aria-label='Open the Researcher Workbench'
      >
        Open the Researcher Workbench
        <FontAwesomeIcon
          icon={faUpRightFromSquare}
          style={{ marginLeft: '0.6rem' }}
        />
      </StyledExternalLink>
    </div>
  );
};
