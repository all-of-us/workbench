import {Component} from '@angular/core';
import * as React from 'react';

import {ReactWrapperBase, withCurrentWorkspace} from 'app/utils';
import {WorkspaceData} from 'app/utils/workspace-data';
import {clusterApi, workspacesApi} from "../services/swagger-fetch-clients";
import {NotebookActionsPopup} from "./notebook-actions-popup";
import {PlaygroundModeIcon} from "../icons/playground-mode-icon";
import {EditComponentReact} from "../icons/edit";
import {Cluster, ClusterStatus} from "../../generated/fetch";
import {urlParamsStore} from "../utils/navigation";
import {notebooksClusterApi} from "../services/notebooks-swagger-fetch-clients";

interface Props {
  workspace: WorkspaceData
}
interface State {
  cluster: Cluster;
  html: string
}

export const InteractiveNotebook = withCurrentWorkspace()(class extends React.Component<Props, State> {

  private pollClusterTimer: NodeJS.Timer;
  private billingProjectId = urlParamsStore.getValue().ns;

  constructor(props) {
    super(props);
    this.state = {
      cluster: null,
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
        console.log("Polled cluster status: " + cluster.status);

        if (cluster.status === ClusterStatus.Stopped) {
          notebooksClusterApi()
            .startCluster(cluster.clusterNamespace, cluster.clusterName)
            .then(resp => console.log('start cluster success'))
            .catch(resp => console.log('start cluster failed'));
        }

        if (cluster.status !== ClusterStatus.Running) {
          repoll();
          return;
        }

        this.setState({cluster: cluster});
      })
      .catch(() => {
        console.log("Poll cluster failed");
        // Also retry on errors
        repoll();
      });
  }

  private onEditClick() {
    this.pollCluster(this.billingProjectId);
  }

  render() {
    return (
      <div>
        <div style={{height: 35, backgroundColor: 'white', borderStyle: 'solid', borderWidth: 'thin', borderColor: '#cdcdcd'}}>
          <div style={{float: 'left', height: '100%', width: 227, color: '#262262', backgroundColor: 'rgba(38,34,98,0.2)', textAlign: 'center', lineHeight: '35px'}}> Preview (Read-Only) </div>
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
        </div>
        <iframe style={{width: '100%', height: 800, border: 0}} srcDoc={this.state.html}>
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
