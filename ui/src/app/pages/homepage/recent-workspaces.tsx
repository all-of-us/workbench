import * as React from 'react';

import {workspacesApi} from 'app/services/swagger-fetch-clients';
import {RecentWorkspace} from 'generated/fetch';
import {SpinnerOverlay} from "../../components/spinners";

interface Props {}
interface State {
  loading: boolean,
  workspaces: RecentWorkspace[],
}

export class RecentWorkspaces extends React.Component<Props, State> {
  constructor(props) {
    super(props);
    this.state = {
      loading: false,
      workspaces: [],
    };
  }

  componentDidMount() {
    this.loadResources();
  }

  async loadResources() {
    try {
      this.setState({loading: true});
      const workspaces = await workspacesApi().getUserRecentWorkspaces();
      this.setState({workspaces});
    } catch (error) {
      console.error(error);
    } finally {
      this.setState({loading: false});
    }
  }

  render() {
    return <div>
      lol
      {this.state.loading && <SpinnerOverlay dark={true} />}
    </div>;
  }
}