import {Component} from '@angular/core';
import * as React from 'react';

import {ReactWrapperBase, withCurrentWorkspace} from 'app/utils';
import {WorkspaceData} from 'app/utils/workspace-data';
import {clusterApi, workspacesApi} from "../services/swagger-fetch-clients";
import {Cluster, ClusterStatus} from "../../generated/fetch";
import {urlParamsStore} from "../utils/navigation";
import {notebooksClusterApi} from "../services/notebooks-swagger-fetch-clients";
import {ClrIcon} from "../components/icons";
import {EditComponentReact} from "../icons/edit";
import {PlaygroundModeIcon} from "../icons/playground-mode-icon";

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

  private pollCluster(billingProjectId): void {
    const repoll = () => {
      this.pollClusterTimer = setTimeout(() => this.pollCluster(billingProjectId), 5000);
    };

    clusterApi().listClusters(billingProjectId)
      .then((body) => {
        const cluster = body.defaultCluster;
        this.setState({ clusterStatus: cluster.status });
        console.log("Polled cluster status: " + cluster.status);

        if (cluster.status === ClusterStatus.Stopped) {
          notebooksClusterApi()
            .startCluster(cluster.clusterNamespace, cluster.clusterName)
            .then(resp => console.log('start cluster success'))
            .catch(resp => console.log('start cluster failed'));
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
    console.log('Switching to edit mode');
  }

  render() {
    return (
      <div>
        <div style={{height: 35, backgroundColor: 'white', borderStyle: 'solid', borderWidth: 'thin', borderColor: '#cdcdcd'}}>
          <div style={{float: 'left', height: '100%', width: 227, color: '#262262', backgroundColor: 'rgba(38,34,98,0.2)', textAlign: 'center', lineHeight: '35px'}}>
            Preview (Read-Only)
          </div>
          {this.state.userRequestedEditMode ? (
            <div style={{
              float: 'left',
              height: '100%',
              width: 550,
              color: '#262262',
              borderLeft: '1px solid rgb(205, 205, 205)',
              backgroundColor: 'rgba(38,34,98,0.05)',
              textAlign: 'center',
              lineHeight: '35px'
            }}>
              <ClrIcon shape="sync" style={{
                marginRight: '5px',
                marginBottom: '3px',
                animation: 'rotation 2s infinite linear'
              }}></ClrIcon>
              Preparing your Jupyter environment. This may take up to 2 minutes.
            </div>) : (<div>
              <div onClick={() => {this.onEditClick();}} style={{cursor: 'pointer', float: 'left', height: '100%', width: 135, color: '#262262', borderLeft: '1px solid rgb(205, 205, 205)', borderRight: '1px solid rgb(205, 205, 205)', backgroundColor: 'rgba(38,34,98,0.05)', textAlign: 'center', lineHeight: '35px'}}>
                <EditComponentReact enableHoverEffect={false} disabled={false} style={{height: '14px', width: '14px', verticalAlign: 'middle', marginLeft: 0, marginRight: '5px', marginBottom: '3px'}} />
                Edit (In Use)
              </div>
              <div style={{float: 'left', height: '100%', width: 210, color: '#262262', backgroundColor: 'rgba(38,34,98,0.05)', textAlign: 'center', lineHeight: '35px'}}>
                <div style={{fill: '#216FB4', width: 20, height: '100%', lineHeight: '52px', marginLeft: '12px', float: 'left'}}>
                  <PlaygroundModeIcon />
                </div>
                <div style={{float: 'left'}}>
                  Run (Playground Mode)
                </div>
              </div>
            </div>)
          }
        </div>
        <iframe style={{width: '100%', height: 800, border: 0}} srcDoc={this.state.html}>
        </iframe>
      </div>
    );
  }

  /*
            <div onClick={() => {this.onEditClick();}} style={{cursor: 'pointer', float: 'left', height: '100%', width: 135, color: '#262262', borderLeft: '1px solid rgb(205, 205, 205)', borderRight: '1px solid rgb(205, 205, 205)', backgroundColor: 'rgba(38,34,98,0.05)', textAlign: 'center', lineHeight: '35px'}}>
            <EditComponentReact enableHoverEffect={false} disabled={false} style={{height: '14px', width: '14px', verticalAlign: 'middle', marginLeft: 0, marginRight: '5px', marginBottom: '3px'}} />
            Edit (In Use)
          </div>
          <div style={{float: 'left', height: '100%', width: 210, color: '#262262', backgroundColor: 'rgba(38,34,98,0.05)', textAlign: 'center', lineHeight: '35px'}}>
            <div style={{fill: '#216FB4', width: 20, height: '100%', lineHeight: '52px', marginLeft: '12px', float: 'left'}}>
              <PlaygroundModeIcon />
            </div>
            <div style={{float: 'left'}}>
              Run (Playground Mode)
            </div>
          </div>
   */
});

@Component({
  template: '<div #root></div>'
})
export class InteractiveNotebookComponent extends ReactWrapperBase {
  constructor() {
    super(InteractiveNotebook, []);
  }
}
