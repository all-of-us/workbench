import {Component} from '@angular/core';
import * as Cookies from 'js-cookie';
import * as fp from 'lodash/fp';
import * as React from 'react';

import {ClrIcon} from 'app/components/icons';
import {TooltipTrigger} from 'app/components/popups';
import {SpinnerOverlay} from 'app/components/spinners';
import {EditComponentReact} from 'app/icons/edit';
import {PlaygroundModeIcon} from 'app/icons/playground-mode-icon';
import {ConfirmPlaygroundModeModal} from 'app/pages/analysis/confirm-playground-mode-modal';
import {NotebookInUseModal} from 'app/pages/analysis/notebook-in-use-modal';
import {workspacesApi} from 'app/services/swagger-fetch-clients';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {reactStyles, ReactWrapperBase, withCurrentWorkspace, withUrlParams} from 'app/utils';
import {AnalyticsTracker} from 'app/utils/analytics';
import {ClusterInitializer} from 'app/utils/cluster-initializer';
import {navigate, userProfileStore} from 'app/utils/navigation';
import {ACTION_DISABLED_INVALID_BILLING} from 'app/utils/strings';
import {WorkspaceData} from 'app/utils/workspace-data';
import {WorkspacePermissionsUtil} from 'app/utils/workspace-permissions';
import {BillingStatus, ClusterStatus} from 'generated/fetch';


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
  previewMessageBase: {
    marginLeft: 'auto',
    marginRight: 'auto',
    marginTop: '56px'
  },
  previewInvalid: {
    color: colorWithWhiteness(colors.dark, .6),
    fontSize: '18px',
    fontWeight: 600,
    lineHeight: '32px',
    maxWidth: '840px',
    textAlign: 'center'
  },
  previewError: {
    background: colors.warning,
    border: '1px solid #ebafa6',
    borderRadius: '5px',
    color: colors.white,
    display: 'flex',
    fontSize: '14px',
    fontWeight: 500,
    maxWidth: '550px',
    padding: '8px',
    textAlign: 'left'
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
  previewErrorMode: PreviewErrorMode;
  previewErrorMessage: string;
}

enum PreviewErrorMode {
  NONE = 'none',
  INVALID = 'invalid',
  ERROR = 'error'
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
        previewErrorMode: PreviewErrorMode.NONE,
        previewErrorMessage: ''
      };
    }

    componentDidMount(): void {
      const {ns, wsid, nbName} = this.props.urlParams;

      workspacesApi().readOnlyNotebook(ns, wsid, nbName).then(html => {
        this.setState({html: html.html});
      }).catch((e) => {
        let previewErrorMode = PreviewErrorMode.ERROR;
        let previewErrorMessage = 'Failed to render preview due to an unknown error, ' +
            'please try reloading or opening the notebook in edit or playground mode.';
        if (e.status === 412) {
          previewErrorMode = PreviewErrorMode.INVALID;
          previewErrorMessage = 'Notebook is too large to display in preview mode, please use edit mode or ' +
              'playground mode to view this notebook.';
        }
        this.setState({previewErrorMode, previewErrorMessage});
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

    private async runCluster(onClusterReady: Function): Promise<void> {
      await ClusterInitializer.initialize({
        workspaceNamespace: this.props.urlParams.ns,
        onStatusUpdate: (status) => this.setState({clusterStatus: status}),
        abortSignal: this.aborter.signal
      });
      onClusterReady();
    }

    private startEditMode() {
      if (this.canStartClusters) {
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
      if (this.canStartClusters) {
        this.setState({userRequestedExecutableNotebook: true});
        this.runCluster(() => { this.navigatePlaygroundMode(); });
      }
    }

    private onPlaygroundModeClick() {
      if (!this.canStartClusters) {
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

    private get billingLocked() {
      return this.props.workspace.billingStatus === BillingStatus.INACTIVE;
    }

    private get canStartClusters() {
      return this.canWrite && !this.billingLocked;
    }

    private get buttonStyleObj() {
      return Object.assign({}, styles.navBarItem,
        this.canStartClusters ? styles.clickable : styles.disabled);
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

    private renderPreviewContents() {
      const {html, previewErrorMode, previewErrorMessage} = this.state;
      if (html) {
        return (<iframe id='notebook-frame' style={styles.previewFrame} srcDoc={html}/>);
      }
      switch (previewErrorMode) {
        case PreviewErrorMode.NONE:
          return (<SpinnerOverlay/>);
        case PreviewErrorMode.INVALID:
          return (<div style={{...styles.previewMessageBase, ...styles.previewInvalid}}>{previewErrorMessage}</div>);
        case PreviewErrorMode.ERROR:
          return (<div style={{...styles.previewMessageBase, ...styles.previewError}}>
            <ClrIcon style={{margin: '0 0.5rem 0 0.25rem'}} className='is-solid'
                     shape='exclamation-triangle' size='30'/>
            {previewErrorMessage}
          </div>);
      }
    }

    render() {
      const {
        lastLockedBy,
        showInUseModal,
        showPlaygroundModeModal,
        userRequestedExecutableNotebook,
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
                    <TooltipTrigger content={this.billingLocked && ACTION_DISABLED_INVALID_BILLING}>
                      <div style={this.buttonStyleObj}
                           onClick={() => {
                             AnalyticsTracker.Notebooks.Edit();
                             this.startEditMode();
                           }}>
                        <EditComponentReact enableHoverEffect={false}
                                            disabled={!this.canStartClusters}
                                            style={styles.navBarIcon}/>
                        Edit {this.notebookInUse && '(In Use)'}
                      </div>
                    </TooltipTrigger>
                    <TooltipTrigger content={this.billingLocked && ACTION_DISABLED_INVALID_BILLING}>
                      <div style={this.buttonStyleObj}
                           onClick={() => {
                             AnalyticsTracker.Notebooks.Run();
                             this.onPlaygroundModeClick();
                           }}>
                        <PlaygroundModeIcon enableHoverEffect={false}
                                            disabled={!this.canStartClusters}
                                            style={styles.navBarIcon}/>
                        Run (Playground Mode)
                      </div>
                    </TooltipTrigger>
                  </div>
              )
            }
          </div>
          <div style={styles.previewDiv}>
            {this.renderPreviewContents()}
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
