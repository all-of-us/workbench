import {Component, Input} from '@angular/core';
import * as React from 'react';

import {ClrIcon} from 'app/components/icons';
import {Spinner, SpinnerOverlay} from 'app/components/spinners';
import {EditComponentReact} from 'app/icons/edit';
import {PlaygroundModeIcon} from 'app/icons/playground-mode-icon';
import {notebooksClusterApi} from 'app/services/notebooks-swagger-fetch-clients';
import {clusterApi, workspacesApi} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {reactStyles, ReactWrapperBase, withCurrentWorkspace} from 'app/utils';
import {navigate, urlParamsStore} from 'app/utils/navigation';
import {WorkspaceData} from 'app/utils/workspace-data';
import {WorkspacePermissionsUtil} from 'app/utils/workspace-permissions';
import {ClusterStatus} from 'generated/fetch';
import {Modal, ModalBody, ModalFooter, ModalTitle} from "../components/modals";
import {Button} from "../components/buttons";
import {CheckBox} from "../components/inputs";
import {ConfirmPlaygroundModeModal} from './confirm-playground-mode-modal';


const styles = reactStyles({
  navBar: {
    display: 'flex',
    height: 40,
    backgroundColor: colors.white,
    border: 'solid thin ' + colors.light, // check this
    fontSize: '16px'
  },
  navBarItem: {
    display: 'flex',
    height: '100%',
    color: colors.primary,
    borderRight: '1px solid ' + colors.light,
    backgroundColor: 'rgba(38,34,98,0.05)',
    alignItems: 'center',
    padding: '0 20px'
  },
  active: {
    backgroundColor: 'rgba(38,34,98,0.2)'
  },
  clickable: {
    cursor: 'pointer'
  },
  disabled: {
    cursor: 'not-allowed',
    opacity: 0.7
  },
  navBarIcon: {
    height: '16px',
    width: '16px',
    marginLeft: 0,
    marginRight: '5px'
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
  billingProjectId: string;
  workspaceName: string;
  notebookName: string;
}

interface State {
  clusterStatus: ClusterStatus;
  userRequestedEditMode: boolean;
  html: string;
}

export const InteractiveNotebook = withCurrentWorkspace()(
  class extends React.Component<Props, State> {

    constructor(props) {
      super(props);
      this.state = {
        userRequestedEditMode: false,
        clusterStatus: ClusterStatus.Unknown,
        html: ''
      };
    }

    componentDidMount(): void {
      workspacesApi().readOnlyNotebook(this.props.billingProjectId,
        this.props.workspaceName, this.props.notebookName)
        .then(html => {
          this.setState({html: html.html});
        });
    }

    private runCluster(): void {
      const retry = () => {
        setTimeout(() => this.runCluster(), 5000);
      };

      clusterApi().listClusters(this.props.billingProjectId)
        .then((body) => {
          const cluster = body.defaultCluster;
          this.setState({clusterStatus: cluster.status});

          if (cluster.status === ClusterStatus.Stopped) {
            notebooksClusterApi()
              .startCluster(cluster.clusterNamespace, cluster.clusterName);
          }

          if (cluster.status === ClusterStatus.Running) {
            this.navigateOldNotebooksPage();
          } else {
            retry();
          }
        })
        .catch(() => {
          retry();
        });
    }

    private onEditClick() {
      if (this.canWrite()) {
        this.setState({userRequestedEditMode: true});
        this.runCluster();
      }
    }

    private navigateOldNotebooksPage() {
      navigate(['workspaces', this.props.billingProjectId, this.props.workspaceName,
        'notebooks', this.props.notebookName]);
    }

    private canWrite() {
      return WorkspacePermissionsUtil.canWrite(this.props.workspace.accessLevel);
    }

    render() {
      return (
        <div>
          <div style={styles.navBar}>
            <div style={{...styles.navBarItem, ...styles.active}}>
              Preview (Read-Only)
            </div>
            {this.state.userRequestedEditMode ? (
              <div style={{...styles.navBarItem}}>
                <ClrIcon shape='sync' style={{...styles.navBarIcon, ...styles.rotate}}></ClrIcon>
                Preparing your Jupyter environment. This may take up to 10 minutes.
              </div>) : (<div>
              <div style={
                Object.assign({}, styles.navBarItem,
                  this.canWrite() ? styles.clickable : styles.disabled)}
                   onClick={() => { this.onEditClick(); }}>
                <EditComponentReact enableHoverEffect={false}
                                    disabled={!this.canWrite()}
                                    style={styles.navBarIcon}/>
                Edit
              </div>
            </div>)}
            <div style={
              Object.assign({}, styles.navBarItem,
                  this.canWrite() ? styles.clickable : styles.disabled)}>
              <div style={{...styles.navBarIcon, fill: '#216FB4', marginBottom: '5px'}}>
                <PlaygroundModeIcon />
              </div>
              Run (Playground Mode)
            </div>
          </div>
          <div style={styles.previewFrame}>
            {this.state.html ?
              (<iframe style={styles.previewFrame} srcDoc={this.state.html}></iframe>) :
              (<SpinnerOverlay/>)}
          </div>
          <ConfirmPlaygroundModeModal onClose={null} onContinue={null}/>
        </div>
      );
    }
  });

@Component({
  template: '<div #root></div>'
})
export class InteractiveNotebookComponent extends ReactWrapperBase {
  @Input() billingProjectId = urlParamsStore.getValue().ns;
  @Input() workspaceName = urlParamsStore.getValue().wsid;
  @Input() notebookName = urlParamsStore.getValue().nbName;

  constructor() {
    super(InteractiveNotebook, ['billingProjectId', 'workspaceName', 'notebookName']);
  }
}
