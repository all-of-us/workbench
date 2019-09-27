import * as React from 'react';

import {workspacesApi} from 'app/services/swagger-fetch-clients';
import {SpinnerOverlay} from "app/components/spinners";
import {WorkspaceCard} from "app/pages/workspace/workspace-card";
import {Profile, RecentWorkspace} from 'generated/fetch';
import {withUserProfile} from "app/utils";

interface State {
  loading: boolean,
  recentWorkspaces: RecentWorkspace[],
}

export const RecentWorkspaces = withUserProfile()
(class extends React.Component<{ profileState: { profile: Profile, reload: Function } }, State> {
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
      this.setState({loading: true});
      const recentWorkspaces = await workspacesApi().getUserRecentWorkspaces();
      this.setState({recentWorkspaces: recentWorkspaces});
    } catch (error) {
      console.error(error);
    } finally {
      this.setState({loading: false});
    }
  }

  render() {
    // Needs a min-height so the spinner will render when loading and position: relative so said spinner will center.
    return <div style={{display: 'flex', marginTop: '1rem', minHeight: 247, position: 'relative'}}>
      {
        this.state.recentWorkspaces.map(recentWorkspace => {
          return <WorkspaceCard
            key={recentWorkspace.workspace.name}
            userEmail={this.props.profileState.profile.username}
            workspace={recentWorkspace.workspace}
            accessLevel={recentWorkspace.accessLevel}
            reload={() => this.loadWorkspaces()}
          />
        })
      }
      {this.state.loading && <SpinnerOverlay dark={true} />}
    </div>;
  }
});