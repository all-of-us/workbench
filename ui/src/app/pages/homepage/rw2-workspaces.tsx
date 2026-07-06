import * as React from 'react';

import { WorkspaceResponse } from 'generated/fetch';

import { FlexColumn, FlexRow } from 'app/components/flex';
import { VwbWorkspaceCard } from 'app/pages/workspace/vwb-workspace-card';
import colors from 'app/styles/colors';

interface Props {
  workspaces: WorkspaceResponse[];
  onChange: () => void;
}

export const VwbWorkspaces = class extends React.Component<Props> {
  get migratedWorkspaces(): WorkspaceResponse[] {
    return this.props.workspaces.filter(
      (wp) =>
        wp.workspace.migrationState === 'FINISHED' &&
        wp.workspace.recoveryState == null
    );
  }

  render() {
    const migratedWorkspaces = this.migratedWorkspaces;

    if (migratedWorkspaces.length === 0) {
      return null;
    }

    return (
      <FlexColumn style={{ marginTop: '2rem' }}>
        <FlexRow style={{ alignItems: 'center' }}>
          <h2 style={{ fontWeight: 600, margin: 0 }}>RW 2.0 Workspaces</h2>
        </FlexRow>

        {migratedWorkspaces.length === 0 ? (
          <div style={{ color: colors.primary, margin: '.5em 0' }}>
            No RW 2.0 workspaces found.
          </div>
        ) : (
          <FlexRow
            style={{
              marginTop: '1.5rem',
              minHeight: 247,
              overflow: 'auto',
            }}
          >
            {migratedWorkspaces.map((wp) => (
              <VwbWorkspaceCard
                key={wp.workspace.namespace}
                workspace={wp.workspace}
                accessLevel={wp.accessLevel}
              />
            ))}
          </FlexRow>
        )}
      </FlexColumn>
    );
  }
};
