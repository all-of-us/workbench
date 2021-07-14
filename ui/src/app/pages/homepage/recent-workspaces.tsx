import * as React from 'react';

import {RecentWorkspace} from 'generated/fetch';

import {FlexRow} from 'app/components/flex';
import {SpinnerOverlay} from 'app/components/spinners';
import {WorkspaceCard} from 'app/pages/workspace/workspace-card';
import {workspacesApi} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';

interface State {
  loading: boolean;
  recentWorkspaces: RecentWorkspace[];
}

export const RecentWorkspaces = (class extends React.Component<{}, State> {
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
    return <div style={{position: 'relative'}}>
      {this.state.loading && <SpinnerOverlay dark={true} />}
      {this.state.recentWorkspaces.length === 0 && !this.state.loading ?
        <div style={{color: colors.primary, margin: '.5em 2em'}}>
          <h2 style={{fontWeight: 600, lineHeight: 1.5}}>Create your first workspace</h2>
          <div>As you create your workspaces, this area will store your most recent workspaces.</div>
          <div>To see all workspaces created, click on <b>See all workspaces</b> to the right.</div>
        </div> :
        <div>
          <FlexRow style={{marginTop: '1rem', minHeight: 247, position: 'relative', overflow: 'auto'}}>
            {
              this.state.recentWorkspaces.map(recentWorkspace => {
                return <WorkspaceCard
                  key={recentWorkspace.workspace.namespace}
                  workspace={recentWorkspace.workspace}
                  accessLevel={recentWorkspace.accessLevel}
                  reload={() => this.loadWorkspaces()}
                />;
              })
            }
          </FlexRow>
        </div>}
    </div>;
  }
});
