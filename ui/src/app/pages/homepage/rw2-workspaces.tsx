import * as React from 'react';

import { VwbWorkspace } from 'generated/fetch';

import { FlexColumn, FlexRow } from 'app/components/flex';
import { SpinnerOverlay } from 'app/components/spinners';
import { VwbWorkspaceCard } from 'app/pages/workspace/vwb-workspace-card';
import { workspacesApi } from 'app/services/swagger-fetch-clients';

type VwbWorkspaceCardModel = VwbWorkspace & {
  role?: 'OWNER' | 'WRITER' | 'READER';
  dataCollection?: string;
  lastChanged?: string;
  createdBy?: string;
};

interface State {
  loading: boolean;
  workspaces: VwbWorkspaceCardModel[];
}

interface Props {
  excludeUserFacingIds?: string[];
  currentUsername?: string;
}

export const VwbWorkspaces = class extends React.Component<Props, State> {
  state: State = {
    loading: true,
    workspaces: [],
  };

  async componentDidMount() {
    try {
      const response = await (workspacesApi() as any).getVwbWorkspaces();

      this.setState({
        workspaces: (response.items ?? []) as VwbWorkspaceCardModel[],
        loading: false,
      });
    } catch (error) {
      console.error('Failed to load RW 2.0 workspaces', error);

      this.setState({
        loading: false,
      });
    }
  }

  render() {
    const { loading, workspaces } = this.state;
    const excludedUserFacingIds = new Set(
      (this.props.excludeUserFacingIds ?? []).map((id) => id.toLowerCase())
    );
    const visibleWorkspaces = workspaces.filter(
      (workspace) =>
        !workspace.userFacingId ||
        !excludedUserFacingIds.has(workspace.userFacingId.toLowerCase())
    );

    if (loading) {
      return <SpinnerOverlay />;
    }

    if (visibleWorkspaces.length === 0) {
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
          {visibleWorkspaces.map((workspace) => (
            <VwbWorkspaceCard
              key={workspace.id}
              workspace={workspace}
              currentUsername={this.props.currentUsername}
            />
          ))}
        </FlexRow>
      </FlexColumn>
    );
  }
};
