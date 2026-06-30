import * as React from 'react';

import {
  MigrationState,
  RecentWorkspace,
  WorkspaceRecoveryStatus,
} from 'generated/fetch';

import { FlexRow } from 'app/components/flex';
import { SpinnerOverlay } from 'app/components/spinners';
import { WorkspaceCard } from 'app/pages/workspace/workspace-card';
import { workspacesApi } from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import { serverConfigStore } from 'app/utils/stores';

interface Props {
  onChange: () => void;
  migrationTestingGroup: boolean;
}
interface State {
  loading: boolean;
  recentWorkspaces: RecentWorkspace[];
}

export const RecentWorkspaces = class extends React.Component<Props, State> {
  constructor(props) {
    super(props);
    this.state = {
      loading: false,
      recentWorkspaces: [],
    };
  }

  componentDidMount() {
    this.loadWorkspaces();
  }

  async loadWorkspaces() {
    try {
      this.setState({ loading: true });
      const recentWorkspaces = await workspacesApi().getUserRecentWorkspaces();
      this.setState({ recentWorkspaces: recentWorkspaces });
    } catch (error) {
      console.error(error);
    } finally {
      this.setState({ loading: false });
    }
  }

  render() {
    // Needs a min-height so the spinner will render when loading and position: relative so said spinner will center.
    const restrictLegacyAccess =
      serverConfigStore.get().config.restrictLegacyAccess;

    const isRestrictedUser =
      restrictLegacyAccess && !this.props.migrationTestingGroup;
    const visibleWorkspaces = isRestrictedUser
      ? this.state.recentWorkspaces.filter(
          (rw) =>
            rw.workspace.migrationState !==
              MigrationState.FINISHED.toString() &&
            [
              WorkspaceRecoveryStatus.NOT_STARTED.toString(),
              WorkspaceRecoveryStatus.RECOVERING.toString(),
              WorkspaceRecoveryStatus.FAILED.toString(),
              null,
            ].includes(rw.workspace.recoveryState)
        )
      : this.state.recentWorkspaces;
    return (
      <div style={{ position: 'relative' }}>
        {this.state.loading && <SpinnerOverlay dark={true} />}
        {visibleWorkspaces.length === 0 && !this.state.loading ? (
          <div style={{ color: colors.primary, margin: '.5em 2em' }}>
            <h2 style={{ fontWeight: 600, lineHeight: 1.5 }}>
              Create your first workspace
            </h2>
            <div>
              As you create your workspaces, this area will store your most
              recent workspaces.
            </div>
            <div>
              To see all workspaces created, click on <b>See all workspaces</b>{' '}
              to the right.
            </div>
          </div>
        ) : (
          <div>
            <FlexRow
              style={{
                marginTop: '1.5rem',
                minHeight: 247,
                position: 'relative',
                overflow: 'auto',
              }}
            >
              {visibleWorkspaces.map((recentWorkspace) => {
                return (
                  <WorkspaceCard
                    key={recentWorkspace.workspace.namespace}
                    workspace={recentWorkspace.workspace}
                    accessLevel={recentWorkspace.accessLevel}
                    reload={() => {
                      this.loadWorkspaces();
                      this.props.onChange();
                    }}
                    tierAccessDisabled={
                      typeof recentWorkspace.workspace.recoveryState ===
                      'undefined'
                    }
                  />
                );
              })}
            </FlexRow>
          </div>
        )}
      </div>
    );
  }
};
