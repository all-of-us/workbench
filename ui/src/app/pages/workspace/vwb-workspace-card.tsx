import * as React from 'react';

import { VwbWorkspace } from 'generated/fetch';

import { environment } from 'environments/environment';
import { StyledExternalLink } from 'app/components/buttons';
import { WorkspaceCardBase } from 'app/components/card';
import { FlexColumn, FlexRow } from 'app/components/flex';
import colors from 'app/styles/colors';
import { reactStyles } from 'app/utils';
import { displayDate } from 'app/utils/dates';

const styles = reactStyles({
  workspaceCard: {
    display: 'flex',
    flexDirection: 'column',
    height: '100%',
  },

  workspaceName: {
    color: colors.primary,
    fontSize: 18,
    fontWeight: 600,
    textDecoration: 'none',
    wordBreak: 'break-word',
    marginBottom: '1rem',
  },

  metadataLabel: {
    fontSize: 12,
    color: '#777',
    fontWeight: 500,
  },

  metadataValue: {
    fontSize: 14,
    color: colors.primary,
  },
});

interface Props {
  workspace: VwbWorkspace;
}

export const VwbWorkspaceCard = ({ workspace }: Props) => {
  const vwbWorkspaceUrl = `${environment.vwbUiUrl}/workspaces/${workspace.userFacingId}`;

  return (
    <WorkspaceCardBase>
      <StyledExternalLink
        href={vwbWorkspaceUrl}
        target='_blank'
        style={{
          textDecoration: 'none',
          color: 'inherit',
          display: 'block',
          height: '100%',
        }}
      >
        <FlexColumn
          style={{
            ...styles.workspaceCard,
            padding: '.75rem',
            cursor: 'pointer',
          }}
          data-test-id='vwb-workspace-card'
        >
          <FlexColumn style={{ marginBottom: 'auto' }}>
            <div
              style={styles.workspaceName}
              data-test-id='workspace-card-name'
            >
              {workspace.displayName}
            </div>
          </FlexColumn>

          <FlexRow style={{ justifyContent: 'space-between' }}>
            <FlexColumn>
              <div style={styles.metadataLabel}>Role</div>
              <div style={styles.metadataValue}>{workspace.role ?? '-'}</div>

              <div style={{ height: 8 }} />

              <div style={styles.metadataLabel}>Data Collection</div>
              <div style={styles.metadataValue}>
                {workspace.dataCollection ?? '-'}
              </div>

              <div style={{ height: 8 }} />

              <div style={styles.metadataLabel}>Last Changed</div>
              <div style={styles.metadataValue}>
                {workspace.lastChanged
                  ? displayDate(Date.parse(workspace.lastChanged))
                  : '-'}
              </div>
            </FlexColumn>
          </FlexRow>
        </FlexColumn>
      </StyledExternalLink>
    </WorkspaceCardBase>
  );
};
