import * as React from 'react';

import { Workspace, WorkspaceAccessLevel } from 'generated/fetch';

import { environment } from 'environments/environment';
import { StyledExternalLink } from 'app/components/buttons';
import { WorkspaceCardBase } from 'app/components/card';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { ControlledTierBadge } from 'app/components/icons';
import colors from 'app/styles/colors';
import { reactStyles } from 'app/utils';
import {
  AccessTierShortNames,
  displayNameForTier,
} from 'app/utils/access-tiers';
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

  permissionBox: {
    color: '#fff',
    height: '1.5rem',
    minWidth: '4.5rem',
    fontSize: 10,
    textAlign: 'center',
    borderRadius: '0.3rem',
    lineHeight: '1.5rem',
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
  workspace: Workspace;
  accessLevel: WorkspaceAccessLevel;
}

export const VwbWorkspaceCard = ({ workspace, accessLevel }: Props) => {
  const vwbWorkspaceUrl = `${environment.vwbUiUrl}/workspaces/${workspace.namespace}`;

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
              {workspace.name}
            </div>
          </FlexColumn>

          <FlexRow style={{ justifyContent: 'space-between' }}>
            <FlexColumn>
              <FlexRow style={{ gap: '6px', alignItems: 'center' }}>
                <div
                  style={{
                    ...styles.permissionBox,
                    backgroundColor:
                      colors.workspacePermissionsHighlights[accessLevel],
                  }}
                >
                  {accessLevel}
                </div>

                {workspace.accessTierShortName ===
                  AccessTierShortNames.Controlled && <ControlledTierBadge />}
              </FlexRow>

              <div style={{ fontSize: 12 }}>
                Data Collection:{' '}
                {displayNameForTier(workspace.accessTierShortName)}
              </div>

              <div style={{ fontSize: 12 }}>
                Last Changed: {displayDate(workspace.lastModifiedTime)}
              </div>
            </FlexColumn>
          </FlexRow>
        </FlexColumn>
      </StyledExternalLink>
    </WorkspaceCardBase>
  );
};
