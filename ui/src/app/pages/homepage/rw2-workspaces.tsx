import * as React from 'react';

import { VwbWorkspace } from 'generated/fetch';

import { FlexColumn, FlexRow } from 'app/components/flex';
import { SpinnerOverlay } from 'app/components/spinners';
import { VwbWorkspaceCard } from 'app/pages/workspace/vwb-workspace-card';

export type VwbWorkspaceCardModel = VwbWorkspace & {
  role?: 'OWNER' | 'WRITER' | 'READER';
  dataCollection?: string;
  lastChanged?: string;
  createdBy?: string;
};

interface Props {
  loading: boolean;
  workspaces: VwbWorkspaceCardModel[];
  currentUsername?: string;
}

export const VwbWorkspaces = ({
  loading,
  workspaces,
  currentUsername,
}: Props) => {
  if (loading) {
    return <SpinnerOverlay />;
  }

  if (workspaces.length === 0) {
    return null;
  }

  return (
    <FlexColumn style={{ marginTop: '2rem' }}>
      <FlexRow style={{ alignItems: 'center' }}>
        <h2 style={{ fontWeight: 600, margin: 0 }}>RW 2.0 Workspaces</h2>
      </FlexRow>

      <FlexRow
        style={{
          marginTop: '1.5rem',
          flexWrap: 'wrap',
        }}
      >
        {workspaces.map((workspace) => (
          <VwbWorkspaceCard
            key={workspace.id}
            workspace={workspace}
            currentUsername={currentUsername}
          />
        ))}
      </FlexRow>
    </FlexColumn>
  );
};
