import {Component} from '@angular/core';
import * as React from 'react';

import {ClrIcon} from 'app/components/icons';
import {EditComponentReact} from 'app/icons/edit';
import {notebooksClusterApi} from 'app/services/notebooks-swagger-fetch-clients';
import {clusterApi, workspacesApi} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {reactStyles, ReactWrapperBase, withCurrentWorkspace} from 'app/utils';
import {navigate, urlParamsStore} from 'app/utils/navigation';
import {WorkspaceData} from 'app/utils/workspace-data';
import {ClusterStatus} from 'generated/fetch';

const styles = reactStyles({
  navBar: {
    height: 35,
    backgroundColor: colors.white,
    border: 'solid thin ' + colors.gray[5]
  },
  navBarItem: {
    float: 'left',
    height: '100%',
    color: colors.blue[9],
    borderRight: '1px solid ' + colors.gray[5],
    backgroundColor: 'rgba(38,34,98,0.05)',
    textAlign: 'center',
    lineHeight: '35px'
  },
  active: {
    backgroundColor: 'rgba(38,34,98,0.2)'
  },
  clickable: {
    cursor: 'pointer'
  },
  navBarIcon: {
    height: '14px',
    width: '14px',
    verticalAlign: 'middle',
    marginLeft: 0,
    marginRight: '5px',
    marginBottom: '3px'
  },
  previewFrame: {
    width: '100%',
    height: 800,
    border: 0
  },
  rotate: {
    animation: 'rotation 2s infinite linear'
  }
});

interface Props {
  workspace: WorkspaceData;
}

interface State {
  clusterStatus: ClusterStatus;
  userRequestedEditMode: boolean;
  html: string;
}

export const InteractiveNotebook = withCurrentWorkspace()(
  class extends React.Component<Props, State> {

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
      workspacesApi().readOnlyNotebook(this.billingProjectId, this.workspaceId, this.nbName)
        .then(html => {
          this.setState({html: html.html});
        });
    }

    private runCluster(): void {
      const retry = () => {
        setTimeout(() => this.runCluster(), 5000);
      };

      clusterApi().listClusters(this.billingProjectId)
        .then((body) => {
          const cluster = body.defaultCluster;
          this.setState({clusterStatus: cluster.status});

          if (cluster.status === ClusterStatus.Stopped) {
            notebooksClusterApi()
              .startCluster(cluster.clusterNamespace, cluster.clusterName);
          }

          if (cluster.status === ClusterStatus.Running) {
            this.onClusterRunning();
          } else {
            retry();
          }
        })
        .catch(() => {
          retry();
        });
    }

    private onEditClick() {
      this.setState({userRequestedEditMode: true});
      this.runCluster();
    }

    private onClusterRunning() {
      navigate(['workspaces', this.billingProjectId, this.workspaceId, 'notebooks', this.nbName]);
    }

    render() {
      return (
        <div>
          <div style={styles.navBar}>
            <div style={{...styles.navBarItem, ...styles.active, width: 227}}>
              Preview (Read-Only)
            </div>
            {this.state.userRequestedEditMode ? (
              <div style={{...styles.navBarItem, width: 550}}>
                <ClrIcon shape='sync' style={{...styles.navBarIcon, ...styles.rotate}}></ClrIcon>
                Preparing your Jupyter environment. This may take up to 10 minutes.
              </div>) : (<div>
              <div onClick={() => {
                this.onEditClick();
              }} style={{...styles.navBarItem, ...styles.clickable, width: 135}}>
                <EditComponentReact enableHoverEffect={false}
                                    disabled={false}
                                    style={{...styles.navBarIcon}}/>
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
