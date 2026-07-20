import * as React from 'react';

import { VwbWorkspace, WorkspaceAccessLevel } from 'generated/fetch';

import { environment } from 'environments/environment';
import { StyledExternalLink } from 'app/components/buttons';
import { WorkspaceCardBase } from 'app/components/card';
import { FlexColumn, FlexRow } from 'app/components/flex';
import {
  ClrIcon,
  ControlledTierBadge,
  RegisteredTierBadge,
} from 'app/components/icons';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import { reactStyles } from 'app/utils';
import { displayDate } from 'app/utils/dates';

const styles = reactStyles({
  workspaceCard: {
    justifyContent: 'flex-end',
    height: '100%',
    position: 'relative',
  },

  workspaceMenuWrapper: {
    paddingTop: '.75rem',
    borderRight: '1px solid',
    borderColor: colorWithWhiteness(colors.dark, 0.6),
    flex: '0 0 1.5rem',
    justifyContent: 'flex-start',
    alignItems: 'center',
  },

  workspaceName: {
    color: colors.accent,
    marginBottom: '.5rem',
    fontSize: 18,
    fontWeight: 600,
    wordBreak: 'break-all',
    textDecoration: 'none',
  },

  permissionBox: {
    color: colors.white,
    height: '1.5rem',
    width: '4.5rem',
    fontSize: 10,
    textAlign: 'center',
    borderRadius: '.3rem',
    padding: 0,
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',
  },
});

interface Props {
  workspace: VwbWorkspace;
}

export const VwbWorkspaceCard = ({ workspace }: Props) => {
  const workspaceUrl = `${environment.vwbUiUrl}/workspaces/${workspace.userFacingId}`;

  const role =
    (workspace.role as WorkspaceAccessLevel) || WorkspaceAccessLevel.READER;

  const isControlledTier = workspace.dataCollection === 'Controlled Tier';
  const isRegisteredTier = workspace.dataCollection === 'Registered Tier';

  return (
    <WorkspaceCardBase>
      <FlexRow style={{ height: '100%' }}>
        {/* Left gray strip (same as Legacy card) */}
        <FlexColumn style={styles.workspaceMenuWrapper} />

        <FlexColumn
          style={{
            ...styles.workspaceCard,
            padding: '.75rem',
          }}
          data-test-id='vwb-workspace-card'
        >
          <FlexColumn style={{ marginBottom: 'auto' }}>
            <StyledExternalLink
              href={workspaceUrl}
              target='_blank'
              style={styles.workspaceName}
            >
              {workspace.displayName}
            </StyledExternalLink>

            <a
              href={workspaceUrl}
              target='_blank'
              rel='noopener noreferrer'
              style={{
                fontSize: '12px',
                color: colors.accent,
                textDecoration: 'none',
                display: 'flex',
                alignItems: 'center',
                gap: '4px',
                marginBottom: '.75rem',
              }}
            >
              Open in RW 2.0
              <ClrIcon shape='pop-out' size={10} />
            </a>
          </FlexColumn>

          <FlexRow style={{ justifyContent: 'space-between' }}>
            <FlexColumn>
              <div
                style={{
                  ...styles.permissionBox,
                  backgroundColor: colors.workspacePermissionsHighlights[role],
                }}
              >
                {workspace.role}
              </div>

              <div style={{ fontSize: 12, marginTop: '.4rem' }}>
                Last Changed:{' '}
                {workspace.lastChanged
                  ? displayDate(Date.parse(workspace.lastChanged))
                  : '-'}
              </div>
            </FlexColumn>

            {(isControlledTier || isRegisteredTier) && (
              <FlexColumn
                style={{
                  justifyContent: 'flex-end',
                  marginLeft: '1.2rem',
                }}
              >
                {isControlledTier && <ControlledTierBadge />}
                {isRegisteredTier && <RegisteredTierBadge />}
              </FlexColumn>
            )}
          </FlexRow>
        </FlexColumn>
      </FlexRow>
    </WorkspaceCardBase>
  );
};
