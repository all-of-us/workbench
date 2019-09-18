import * as React from 'react';
import {workspacesApi} from "app/services/swagger-fetch-clients";

interface Props {}
interface State {
  loading: boolean,
  workspaces: UserRecentWorkspace[],
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
    return <div>lol</div>;
  }
}