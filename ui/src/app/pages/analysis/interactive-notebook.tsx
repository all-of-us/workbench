import {Component} from '@angular/core';
import * as Cookies from 'js-cookie';
import * as fp from 'lodash/fp';
import * as React from 'react';

import {ClrIcon} from 'app/components/icons';
import {SpinnerOverlay} from 'app/components/spinners';
import {EditComponentReact} from 'app/icons/edit';
import {PlaygroundModeIcon} from 'app/icons/playground-mode-icon';
import {ConfirmPlaygroundModeModal} from 'app/pages/analysis/confirm-playground-mode-modal';
import {NotebookInUseModal} from 'app/pages/analysis/notebook-in-use-modal';
import {notebooksClusterApi} from 'app/services/notebooks-swagger-fetch-clients';
import {clusterApi, workspacesApi} from 'app/services/swagger-fetch-clients';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {reactStyles, ReactWrapperBase, withCurrentWorkspace, withUrlParams} from 'app/utils';
import {isAbortError} from 'app/utils/errors';
import {navigate, userProfileStore} from 'app/utils/navigation';
import {WorkspaceData} from 'app/utils/workspace-data';
import {WorkspacePermissionsUtil} from 'app/utils/workspace-permissions';
import {ClusterStatus} from 'generated/fetch';


const styles = reactStyles({
  navBar: {
    display: 'flex',
    height: 40,
    backgroundColor: colors.white,
    border: 'solid thin ' + colorWithWhiteness(colors.dark, .75),
    fontSize: '16px'
  },
  navBarItem: {
    display: 'flex',
    height: '100%',
    color: colors.primary,
    borderRight: '1px solid ' + colorWithWhiteness(colors.dark, .75),
    backgroundColor: 'rgba(38,34,98,0.05)',
    alignItems: 'center',
    padding: '0 20px',
    textTransform: 'uppercase'
  },
  active: {
    backgroundColor: 'rgba(38,34,98,0.2)'
  },
  clickable: {
    cursor: 'pointer'
  },
  disabled: {
    cursor: 'not-allowed',
    color: colorWithWhiteness(colors.dark, 0.6)
  },
  navBarIcon: {
    height: '16px',
    width: '16px',
    marginLeft: 0,
    marginRight: '5px'
  },
  previewDiv: {
    width: '100%',
    border: 0
  },
  previewFrame: {
    width: '100%',
    height: 'calc(100% - 40px)',
    position: 'absolute',
    border: 0
  },
  rotate: {
    animation: 'rotation 2s infinite linear'
  }
});

interface Props {
  workspace: WorkspaceData;
  urlParams: any;
}

interface State {
  clusterStatus: ClusterStatus;
  html: string;
  lastLockedBy: string;
  lockExpirationTime: number;
  showInUseModal: boolean;
  showPlaygroundModeModal: boolean;
  userRequestedExecutableNotebook: boolean;
}

export const InteractiveNotebook = fp.flow(withUrlParams(), withCurrentWorkspace())(
  class extends React.Component<Props, State> {
    private runClusterTimer: NodeJS.Timeout;
    private aborter = new AbortController();

    constructor(props) {
      super(props);
      this.state = {
        clusterStatus: ClusterStatus.Unknown,
        html: '',
        lastLockedBy: '',
        lockExpirationTime: 0,
        showInUseModal: false,
        showPlaygroundModeModal: false,
        userRequestedExecutableNotebook: false,
      };
    }

    componentDidMount(): void {
      const {ns, wsid, nbName} = this.props.urlParams;

      workspacesApi().readOnlyNotebook(ns, wsid, nbName).then(html => {
        this.setState({html: html.html});
      });

      workspacesApi().getNotebookLockingMetadata(ns, wsid, nbName).then((resp) => {
        this.setState({
          lastLockedBy: resp.lastLockedBy,
          lockExpirationTime: resp.lockExpirationTime
        });
      });
    }

    componentWillUnmount(): void {
      clearTimeout(this.runClusterTimer);
      this.aborter.abort();
    }

    private runCluster(onClusterReady: Function): void {
      const retry = () => {
        this.runClusterTimer = setTimeout(() => this.runCluster(onClusterReady), 5000);
      };

      clusterApi().listClusters(this.props.urlParams.ns, this.props.urlParams.wsid, {
        signal: this.aborter.signal
      }).then((body) => {
          const cluster = body.defaultCluster;
          this.setState({clusterStatus: cluster.status});

          if (cluster.status === ClusterStatus.Stopped) {
            notebooksClusterApi()
              .startCluster(cluster.clusterNamespace, cluster.clusterName);
          }

          if (cluster.status === ClusterStatus.Running) {
            onClusterReady();
          } else {
            retry();
          }
        })
        .catch((e: Error) => {
          if (isAbortError(e)) {
            return;
          }
          // TODO(RW-3097): Backoff, or don't retry forever.
          retry();
        });
    }

    private startEditMode() {
      if (this.canWrite) {
        if (!this.notebookInUse) {
          this.setState({userRequestedExecutableNotebook: true});
          this.runCluster(() => { this.navigateEditMode(); });
        } else {
          this.setState({
            showInUseModal: true
          });
        }
      }
    }



    private startPlaygroundMode() {
      if (this.canWrite) {
        this.setState({userRequestedExecutableNotebook: true});
        this.runCluster(() => { this.navigatePlaygroundMode(); });
      }
    }

    private onPlaygroundModeClick() {
      if (!this.canWrite) {
        return;
      }
      if (Cookies.get(ConfirmPlaygroundModeModal.DO_NOT_SHOW_AGAIN) === String(true)) {
        this.startPlaygroundMode();
      } else {
        this.setState({showPlaygroundModeModal: true});
      }
    }

    private navigateOldNotebooksPage(playgroundMode: boolean) {
      const queryParams = {
        playgroundMode: playgroundMode
      };

      navigate(['workspaces', this.props.urlParams.ns, this.props.urlParams.wsid,
        'notebooks', this.props.urlParams.nbName], {'queryParams': queryParams});
    }

    private navigatePlaygroundMode() {
      this.navigateOldNotebooksPage(true);
    }

    private navigateEditMode() {
      this.navigateOldNotebooksPage(false);
    }

    private get canWrite() {
      return WorkspacePermissionsUtil.canWrite(this.props.workspace.accessLevel);
    }

    private get buttonStyleObj() {
      return Object.assign({}, styles.navBarItem,
        this.canWrite ? styles.clickable : styles.disabled);
    }

    private cloneNotebook() {
      const {ns, wsid, nbName} = this.props.urlParams;
      workspacesApi().cloneNotebook(ns, wsid, nbName).then((notebook) => {
        navigate(['workspaces', ns, wsid, 'notebooks', encodeURIComponent(notebook.name)]);
      });
    }

    private get notebookInUse() {
      const {lastLockedBy, lockExpirationTime} = this.state;
      return lastLockedBy !== null && new Date().getTime()  < lockExpirationTime
        && lastLockedBy !== userProfileStore.getValue().profile.username;
    }

    private renderNotebookText() {
      switch (this.state.clusterStatus) {
        case ClusterStatus.Starting:
        case ClusterStatus.Stopping:
        case ClusterStatus.Stopped:
          return 'Resuming your Jupyter environment. This may take up to 1 minute.';
        case ClusterStatus.Deleting:
        case ClusterStatus.Creating:
        case ClusterStatus.Deleted:
          return 'Preparing your Jupyter environment. This may take up to 10 minutes.';
        case ClusterStatus.Error:
          return 'Error creating your jupyter environment. Please try clicking' +
            ' the reset notebook server on Workspace About Page.';
        default:
          return 'Connecting to the notebook server';

      }
    }

    render() {
      const {
        html,
        lastLockedBy,
        showInUseModal,
        showPlaygroundModeModal,
        userRequestedExecutableNotebook
      } = this.state;
      return (
        <div>
          <div style={styles.navBar}>
            <div style={{...styles.navBarItem, ...styles.active}}>
              Preview (Read-Only)
            </div>
            {userRequestedExecutableNotebook ? (
              <div style={{...styles.navBarItem, textTransform: 'none'}}>
                <ClrIcon shape='sync' style={{...styles.navBarIcon, ...styles.rotate}}/>
                {this.renderNotebookText()}
              </div>) : (
              <div style={{display: 'flex'}}>
                <div style={this.buttonStyleObj}
                     onClick={() => { this.startEditMode(); }}>
                  <EditComponentReact enableHoverEffect={false}
                                      disabled={!this.canWrite}
                                      style={styles.navBarIcon}/>
                  Edit {this.notebookInUse && '(In Use)'}
                </div>
                <div style={this.buttonStyleObj}
                     onClick={() => { this.onPlaygroundModeClick(); }}>
                  <PlaygroundModeIcon enableHoverEffect={false} disabled={!this.canWrite}
                                      style={styles.navBarIcon}/>
                  Run (Playground Mode)
                </div>
              </div>)
            }
          </div>
          <div style={styles.previewDiv}>
            {html ?
              (<iframe id='notebook-frame' style={styles.previewFrame} srcDoc={html}/>) :
              (<SpinnerOverlay/>)}
          </div>
          {showPlaygroundModeModal &&
            <ConfirmPlaygroundModeModal
              onCancel={() => {
                this.setState({showPlaygroundModeModal: false}); }}
              onContinue={() => {
                this.setState({showPlaygroundModeModal: false});
                this.startPlaygroundMode();
              }}/>}
          {showInUseModal &&
          <NotebookInUseModal
            email={lastLockedBy}
            onCancel={() => {
              this.setState({showInUseModal: false}); }}
            onCopy={() => {this.cloneNotebook(); }}
            onPlaygroundMode={() => {
              this.setState({showInUseModal: false});
              this.startPlaygroundMode();
            }}>
          </NotebookInUseModal>}
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
