import * as React from 'react';

import { VwbWorkspace } from 'generated/fetch';

import { FlexColumn, FlexRow } from 'app/components/flex';
import { SpinnerOverlay } from 'app/components/spinners';
import { VwbWorkspaceCard } from 'app/pages/workspace/vwb-workspace-card';
import { workspacesApi } from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';

interface State {
  loading: boolean;
  workspaces: VwbWorkspace[];
}

export const VwbWorkspaces = class extends React.Component<
  Record<string, never>,
  State
> {
  state: State = {
    loading: true,
    workspaces: [],
  };

  async componentDidMount() {
    try {
      const response = await workspacesApi().getVwbWorkspaces();

      this.setState({
        workspaces: response.items ?? [],
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

        {workspaces.length === 0 ? (
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
            {workspaces.map((workspace) => (
              <VwbWorkspaceCard key={workspace.id} workspace={workspace} />
            ))}
          </FlexRow>
        )}
      </FlexColumn>
    );
  }
};
