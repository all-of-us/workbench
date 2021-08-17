import * as Cookies from 'js-cookie';
import * as fp from 'lodash/fp';
import * as React from 'react';

import {IconButton} from 'app/components/buttons';
import {ClrIcon} from 'app/components/icons';
import {PlaygroundIcon} from 'app/components/icons';
import {TooltipTrigger} from 'app/components/popups';
import {SpinnerOverlay} from 'app/components/spinners';
import {WithSpinnerOverlayProps} from 'app/components/with-spinner-overlay';
import {EditComponentReact} from 'app/icons/edit';
import {ConfirmPlaygroundModeModal} from 'app/pages/analysis/confirm-playground-mode-modal';
import {NotebookInUseModal} from 'app/pages/analysis/notebook-in-use-modal';
import {workspacesApi} from 'app/services/swagger-fetch-clients';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {reactStyles, withCurrentWorkspace, withUrlParams} from 'app/utils';
import {AnalyticsTracker} from 'app/utils/analytics';
import {navigate} from 'app/utils/navigation';
import {withRuntimeStore} from 'app/utils/runtime-utils';
import {maybeInitializeRuntime} from 'app/utils/runtime-utils';
import {profileStore, RuntimeStore} from 'app/utils/stores';
import {ACTION_DISABLED_INVALID_BILLING} from 'app/utils/strings';
import {WorkspaceData} from 'app/utils/workspace-data';
import {WorkspacePermissionsUtil} from 'app/utils/workspace-permissions';
import {BillingStatus, RuntimeStatus} from 'generated/fetch';


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
    lineHeight: '16px',
    marginRight: '5px'
  },
  previewDiv: {
    width: '100%',
    border: 0
  },
  previewFrame: {
    // at 100% the frame's scrollbar is hidden under the minimized sidebar.  Subtract the sidebar's width.
    width: 'calc(100% - 45px)',
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

interface Props extends WithSpinnerOverlayProps {
  workspace: WorkspaceData;
  urlParams: any;
  runtimeStore: RuntimeStore;
}

interface State {
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

export const InteractiveNotebook = fp.flow(
  withUrlParams(),
  withRuntimeStore(),
  withCurrentWorkspace()
)(
  class extends React.Component<Props, State> {
    private pollAborter = new AbortController();

    constructor(props) {
      super(props);
      this.state = {
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
      const {urlParams: {ns, wsid, nbName}, hideSpinner} = this.props;
      hideSpinner();

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
      this.pollAborter.abort();
    }

    private async runRuntime(onRuntimeReady: Function): Promise<void> {
      await maybeInitializeRuntime(this.props.urlParams.ns, this.pollAborter.signal);
      onRuntimeReady();
    }

    private startEditMode() {
      if (this.canStartRuntimes) {
        if (!this.notebookInUse) {
          this.setState({userRequestedExecutableNotebook: true});
          this.runRuntime(() => { this.navigateEditMode(); });
        } else {
          this.setState({
            showInUseModal: true
          });
        }
      }
    }

    private startPlaygroundMode() {
      if (this.canStartRuntimes) {
        this.setState({userRequestedExecutableNotebook: true});
        this.runRuntime(() => { this.navigatePlaygroundMode(); });
      }
    }

    private onPlaygroundModeClick() {
      if (!this.canStartRuntimes) {
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

    private get canStartRuntimes() {
      return this.canWrite && !this.billingLocked;
    }

    private get buttonStyleObj() {
      return Object.assign({}, styles.navBarItem,
        this.canStartRuntimes ? styles.clickable : styles.disabled);
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
        && lastLockedBy !== profileStore.get().profile.username;
    }

    private renderNotebookText() {
      const {status = RuntimeStatus.Unknown} = this.props.runtimeStore.runtime || {};
      switch (status) {
        case RuntimeStatus.Starting:
        case RuntimeStatus.Stopping:
        case RuntimeStatus.Stopped:
          return 'Resuming your Jupyter environment. This may take up to 1 minute.';
        case RuntimeStatus.Deleting:
        case RuntimeStatus.Creating:
        case RuntimeStatus.Deleted:
          return 'Preparing your Jupyter environment. This may take up to 5 minutes.';
        case RuntimeStatus.Error:
          return 'Error creating your analysis environment. Please delete or ' +
                 'recreate via the compute configuration panel in the sidebar.';
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
                                            disabled={!this.canStartRuntimes}
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
                        <IconButton icon={PlaygroundIcon}
                                    disabled={!this.canStartRuntimes}
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

