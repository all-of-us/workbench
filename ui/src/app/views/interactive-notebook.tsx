import {Component} from '@angular/core';
import * as React from 'react';

import {reactStyles, ReactWrapperBase, withCurrentWorkspace} from 'app/utils';
import {WorkspaceData} from 'app/utils/workspace-data';
import {clusterApi, workspacesApi} from "../services/swagger-fetch-clients";
import {ClusterStatus} from "../../generated/fetch";
import {navigate, urlParamsStore} from "../utils/navigation";
import {notebooksClusterApi} from "../services/notebooks-swagger-fetch-clients";
import {ClrIcon} from "../components/icons";
import {EditComponentReact} from "../icons/edit";
import colors from "../styles/colors";

const styles = reactStyles({
  navBar: {
    height: 35,
    backgroundColor: 'white',
    border: 'solid thin #cdcdcd'
  },
  navBarPreview: {
    width: 227,
    float: 'left',
    height: '100%',
    color: colors.blue[9],
    backgroundColor: 'rgba(38,34,98,0.2)',
    borderRight: '1px solid rgb(205, 205, 205)',
    textAlign: 'center',
    lineHeight: '35px'
  },
  navBarPreparing: {
    width: 550,
    float: 'left',
    height: '100%',
    color: colors.blue[9],
    backgroundColor: 'rgba(38,34,98,0.05)',
    borderRight: '1px solid rgb(205, 205, 205)',
    textAlign: 'center',
    lineHeight: '35px'
  },
  navBarEdit: {
    cursor: 'pointer',
    width: 135,
    float: 'left',
    height: '100%',
    color: colors.blue[9],
    borderRight: '1px solid rgb(205, 205, 205)',
    backgroundColor: 'rgba(38,34,98,0.05)',
    textAlign: 'center',
    lineHeight: '35px'
  },
  navBarEditIcon: {
    height: '14px',
    width: '14px',
    verticalAlign: 'middle',
    marginLeft: 0
  },
  previewFrame: {
    width: '100%',
    height: 800,
    border: 0
  },
  navBarIcon: {
    marginRight: '5px',
    marginBottom: '3px'
  },
  rotate: {
    animation: 'rotation 2s infinite linear'
  }
});

interface Props {
  workspace: WorkspaceData
}
interface State {
  clusterStatus: ClusterStatus,
  userRequestedEditMode: boolean,
  html: string
}

export const InteractiveNotebook = withCurrentWorkspace()(class extends React.Component<Props, State> {

  private pollClusterTimer: NodeJS.Timer;
  private billingProjectId = urlParamsStore.getValue().ns;
  private workspaceId = urlParamsStore.getValue().wsid;
  private nbName = urlParamsStore.getValue().nbName;

  constructor(props) {
    super(props);
    this.state = {
      userRequestedEditMode: false,
      clusterStatus: ClusterStatus.Unknown,
      html: ''
    };
  }

  componentDidMount(): void {
    workspacesApi().readOnlyNotebook("aou-rw-local1-93b1df18", "newbuffer", "test.ipynb")
      .then(html => {this.setState({html: html.html})})
      .catch(resp => console.error(resp));
  }

  // TODO eric: Refactor from reset cluster button
  private pollCluster(billingProjectId): void {
    const repoll = () => {
      this.pollClusterTimer = setTimeout(() => this.pollCluster(billingProjectId), 5000);
    };

    clusterApi().listClusters(billingProjectId)
      .then((body) => {
        const cluster = body.defaultCluster;
        this.setState({ clusterStatus: cluster.status });

        if (cluster.status === ClusterStatus.Stopped) {
          notebooksClusterApi()
            .startCluster(cluster.clusterNamespace, cluster.clusterName);
        }

        if (cluster.status === ClusterStatus.Running) {
          this.onClusterRunning();
        } else {
          repoll();
          return;
        }

      })
      .catch(() => {
        console.log("Poll cluster failed");
        // Also retry on errors
        repoll();
      });
  }

  private onEditClick() {
    this.setState({ userRequestedEditMode : true });
    this.pollCluster(this.billingProjectId);
  }

  private onClusterRunning() {
    navigate(['workspaces', this.billingProjectId, this.workspaceId, 'notebooks', this.nbName]);
  }

  render() {
    return (
      <div>
        <div style={styles.navBar}>
          <div style={styles.navBarPreview}>
            Preview (Read-Only)
          </div>
          {this.state.userRequestedEditMode ? (
            <div style={styles.navBarPreparing}>
              <ClrIcon shape="sync" style={{...styles.navBarIcon, ...styles.rotate}}></ClrIcon>
              Preparing your Jupyter environment. This may take up to 2 minutes.
            </div>) : (<div>
              <div onClick={() => {this.onEditClick();}} style={styles.navBarEdit}>
                <EditComponentReact enableHoverEffect={false} disabled={false} style={{...styles.navBarIcon, ...styles.navBarEditIcon}} />
                Edit
              </div>
            </div>)
          }
        </div>
        <iframe style={styles.previewFrame} srcDoc={this.state.html}>
        </iframe>
      </div>
    );
  }
});

@Component({
  template: '<div #root></div>'
})
export class InteractiveNotebookComponent extends ReactWrapperBase {
  constructor() {
    super(InteractiveNotebook, []);
  }
}
