import * as React from 'react';

import {Profile, RecentWorkspace} from 'generated/fetch';

import {FlexRow} from 'app/components/flex';
import {WorkspaceCard} from 'app/pages/workspace/workspace-card';
import {workspacesApi} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {withUserProfile} from 'app/utils';

interface State {
  loading: boolean;
  recentWorkspaces: RecentWorkspace[];
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
    return <React.Fragment>
        {!this.state.loading && this.state.recentWorkspaces.length === 0 ? (
          <div style={{color: colors.primary, padding: '10px 30px'}}>
            <h2 style={{fontWeight: 600}}>Create your first Workspace</h2>
            <div>As you create your workspaces, this area will store your most recent workspaces.</div>
            <div>To see all workspaces created, click on <b>See all Workspaces</b> to the right.</div>
          </div>) : (<div>
          <FlexRow style={{marginTop: '1rem', minHeight: 247, position: 'relative'}}>
            {
              this.state.recentWorkspaces.map(recentWorkspace => {
                return <WorkspaceCard
                  key={recentWorkspace.workspace.name}
                  userEmail={this.props.profileState.profile.username}
                  workspace={recentWorkspace.workspace}
                  accessLevel={recentWorkspace.accessLevel}
                  reload={() => this.loadWorkspaces()}
                />;
              })
            }
          </FlexRow>
        </div>)
        }
    </React.Fragment>;
  }
});

//       {this.state.loading && <SpinnerOverlay dark={true} />}
