import * as React from 'react';
import { RouteComponentProps, withRouter } from 'react-router-dom';
import * as fp from 'lodash/fp';

import { Runtime, RuntimeStatus } from 'generated/fetch';

import { cond } from '@terra-ui-packages/core-utils';
import { UIAppType } from 'app/components/apps-panel/utils';
import { ClrIcon } from 'app/components/icons';
import { TooltipTrigger } from 'app/components/popups';
import { RuntimeInitializerModal } from 'app/components/runtime-initializer-modal';
import { SpinnerOverlay } from 'app/components/spinners';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import { EditComponentReact } from 'app/icons/edit';
import { NotebookInUseModal } from 'app/pages/analysis/notebook-in-use-modal';
import { analysisTabName } from 'app/routing/utils';
import { notebooksApi } from 'app/services/swagger-fetch-clients';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import { hasNewValidProps, reactStyles, withCurrentWorkspace } from 'app/utils';
import { AnalyticsTracker } from 'app/utils/analytics';
import { InitialRuntimeNotFoundError } from 'app/utils/leo-runtime-initializer';
import { NavigationProps } from 'app/utils/navigation';
import { withRuntimeStore } from 'app/utils/runtime-hooks';
import {
  ComputeSecuritySuspendedError,
  maybeInitializeRuntime,
  RUNTIME_ERROR_STATUS_MESSAGE_SHORT,
  RuntimeStatusError,
  withUserAppsStore,
} from 'app/utils/runtime-utils';
import {
  MatchParams,
  profileStore,
  RuntimeStore,
  UserAppsStore,
} from 'app/utils/stores';
import { ACTION_DISABLED_INVALID_BILLING } from 'app/utils/strings';
import { openAppOrConfigPanel } from 'app/utils/user-apps-utils';
import { withNavigation } from 'app/utils/with-navigation-hoc';
import { WorkspaceData } from 'app/utils/workspace-data';
import { WorkspacePermissionsUtil } from 'app/utils/workspace-permissions';
import { isValidBilling } from 'app/utils/workspace-utils';

import {
  ErrorMode,
  NotebookFrameError,
  SecuritySuspendedMessage,
} from './notebook-frame-error';
import { getAppInfoFromFileName } from './util';

const styles = reactStyles({
  navBar: {
    display: 'flex',
    height: 40,
    backgroundColor: colors.white,
    border: 'solid thin ' + colorWithWhiteness(colors.dark, 0.75),
    fontSize: '16px',
  },
  navBarItem: {
    display: 'flex',
    height: '100%',
    color: colors.primary,
    borderRight: '1px solid ' + colorWithWhiteness(colors.dark, 0.75),
    backgroundColor: 'rgba(38,34,98,0.05)',
    alignItems: 'center',
    padding: '0 20px',
    textTransform: 'uppercase',
  },
  active: {
    backgroundColor: 'rgba(38,34,98,0.2)',
  },
  clickable: {
    cursor: 'pointer',
  },
  disabled: {
    cursor: 'not-allowed',
    color: colorWithWhiteness(colors.dark, 0.6),
  },
  navBarIcon: {
    height: '16px',
    width: '16px',
    lineHeight: '16px',
    marginRight: '5px',
  },
  previewDiv: {
    width: '100%',
    border: 0,
  },
  previewFrame: {
    // at 100% the frame's scrollbar is hidden under the minimized sidebar.  Subtract the sidebar's width.
    width: 'calc(100% - 45px)',
    height: 'calc(100% - 40px)',
    position: 'absolute',
    border: 0,
  },
  rotate: {
    animation: 'rotation 2s infinite linear',
  },
});

interface Props
  extends WithSpinnerOverlayProps,
    NavigationProps,
    RouteComponentProps<MatchParams> {
  workspace: WorkspaceData;
  runtimeStore: RuntimeStore;
  userAppsStore: UserAppsStore;
}

interface State {
  html: string;
  lastLockedBy: string;
  lockExpirationTime: number;
  showInUseModal: boolean;
  userRequestedExecutableNotebook: boolean;
  runtimeInitializerDefault: Runtime;
  resolveRuntimeInitializer: (Runtime) => void;
  error: Error;
}

export const InteractiveNotebook = fp.flow(
  withRuntimeStore(),
  withUserAppsStore(),
  withCurrentWorkspace(),
  withNavigation,
  withRouter
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
        userRequestedExecutableNotebook: false,
        runtimeInitializerDefault: null,
        resolveRuntimeInitializer: null,
        error: null,
      };
    }

    componentDidMount(): void {
      this.props.hideSpinner();

      const {
        match: {
          params: { ns, terraName, nbName },
        },
      } = this.props;
      if (ns && terraName && nbName) {
        this.loadNotebook();
      }
    }

    componentDidUpdate(prevProps: Readonly<Props>) {
      if (
        hasNewValidProps(this.props, prevProps, [
          (p) => p.match.params.ns,
          (p) => p.match.params.terraName,
          (p) => p.match.params.nbName,
        ])
      ) {
        this.loadNotebook();
      }
    }

    componentWillUnmount(): void {
      this.pollAborter.abort();
      if (this.state.resolveRuntimeInitializer) {
        this.state.resolveRuntimeInitializer(null);
      }
    }

    async loadNotebook() {
      const { ns, terraName, nbName } = this.props.match.params;
      try {
        const { html } = await notebooksApi().readOnlyNotebook(
          ns,
          terraName,
          nbName
        );
        this.setState({ html });
      } catch (e) {
        this.setState({ error: e });
      }

      notebooksApi()
        .getNotebookLockingMetadata(ns, terraName, nbName)
        .then((resp) => {
          this.setState({
            lastLockedBy: resp.lastLockedBy,
            lockExpirationTime: resp.lockExpirationTime,
          });
        });
    }

    private async runRuntime(
      onRuntimeReady: Function,
      targetRuntime?: Runtime
    ): Promise<void> {
      this.setState({ userRequestedExecutableNotebook: true });
      try {
        await maybeInitializeRuntime(
          this.props.match.params.ns,
          this.pollAborter.signal,
          targetRuntime
        );
        onRuntimeReady();
      } catch (e) {
        this.setState({ userRequestedExecutableNotebook: false });
        if (e instanceof InitialRuntimeNotFoundError) {
          // By awaiting the promise here, we're effectively blocking on the
          // user's input to the runtime initializer modal. We invoke this
          // callback with the targetRuntime configuration, or null if the user
          // has decided to cancel or change their configuration.
          const runtimeToCreate = await new Promise((resolve) => {
            this.setState({
              runtimeInitializerDefault: e.defaultRuntime,
              resolveRuntimeInitializer: resolve,
            });
          });
          if (runtimeToCreate) {
            return this.runRuntime(onRuntimeReady, runtimeToCreate);
          }
        } else {
          this.setState({ error: e });
        }
      }
    }

    private navigateEditMode() {
      this.props.navigate([
        'workspaces',
        this.props.match.params.ns,
        this.props.match.params.terraName,
        analysisTabName,
        this.props.match.params.nbName,
      ]);
    }

    private get canWrite() {
      return WorkspacePermissionsUtil.canWrite(
        this.props.workspace.accessLevel
      );
    }

    private get billingLocked() {
      return !isValidBilling(this.props.workspace);
    }

    private get canStartRuntimes() {
      return this.canWrite && !this.billingLocked;
    }

    private get buttonStyleObj() {
      return Object.assign(
        {},
        styles.navBarItem,
        this.canStartRuntimes ? styles.clickable : styles.disabled
      );
    }

    private cloneNotebook() {
      const { ns, terraName, nbName } = this.props.match.params;
      notebooksApi()
        .cloneNotebook(ns, terraName, nbName)
        .then((notebook) => {
          this.props.navigate([
            'workspaces',
            ns,
            terraName,
            analysisTabName,
            encodeURIComponent(notebook.name),
          ]);
        });
    }

    private get notebookInUse() {
      const { lastLockedBy, lockExpirationTime } = this.state;
      return (
        lastLockedBy !== null &&
        new Date().getTime() < lockExpirationTime &&
        lastLockedBy !== profileStore.get().profile.username
      );
    }

    private renderNotebookText() {
      const { status = RuntimeStatus.UNKNOWN } =
        this.props.runtimeStore.runtime || {};
      switch (status) {
        case RuntimeStatus.STARTING:
        case RuntimeStatus.STOPPING:
        case RuntimeStatus.STOPPED:
          return 'Resuming your Jupyter environment. This may take up to 1 minute.';
        case RuntimeStatus.DELETING:
        case RuntimeStatus.CREATING:
          return 'Preparing your Jupyter environment. This may take up to 5 minutes.';
        case RuntimeStatus.ERROR:
          return (
            'Error creating your analysis environment. Please delete or ' +
            'recreate via the compute configuration panel in the sidebar.'
          );
        default:
          return 'Connecting to the notebook server';
      }
    }

    render() {
      const {
        lastLockedBy,
        showInUseModal,
        userRequestedExecutableNotebook,
        runtimeInitializerDefault,
        resolveRuntimeInitializer,
        error,
      } = this.state;
      const closeRuntimeInitializerModal = (r?: Runtime) => {
        resolveRuntimeInitializer(r);
        this.setState({
          runtimeInitializerDefault: null,
          resolveRuntimeInitializer: null,
        });
      };
      return (
        <div>
          <div style={styles.navBar}>
            <div style={{ ...styles.navBarItem, ...styles.active }}>
              Preview (Read-Only)
            </div>
            {cond<React.ReactNode>(
              [
                !!error &&
                  // don't show executable button (Edit) when notebooks cannot be executed
                  (error instanceof ComputeSecuritySuspendedError ||
                    error instanceof RuntimeStatusError),
                () => null,
              ],
              [
                userRequestedExecutableNotebook,
                () => (
                  <div style={{ ...styles.navBarItem, textTransform: 'none' }}>
                    <ClrIcon
                      shape='sync'
                      style={{ ...styles.navBarIcon, ...styles.rotate }}
                    />
                    {this.renderNotebookText()}
                  </div>
                ),
              ],
              () => (
                <div style={{ display: 'flex' }}>
                  <TooltipTrigger
                    content={
                      this.billingLocked && ACTION_DISABLED_INVALID_BILLING
                    }
                  >
                    <div
                      style={this.buttonStyleObj}
                      onClick={() => {
                        AnalyticsTracker.Notebooks.Edit();
                        this.startEditMode();
                      }}
                    >
                      <EditComponentReact
                        enableHoverEffect={false}
                        disabled={!this.canStartRuntimes}
                        style={styles.navBarIcon}
                      />
                      Edit {this.notebookInUse && '(In Use)'}
                    </div>
                  </TooltipTrigger>
                </div>
              )
            )}
          </div>
          <div style={styles.previewDiv}>{this.renderPreviewContents()}</div>
          {showInUseModal && (
            <NotebookInUseModal
              email={lastLockedBy}
              onCancel={() => {
                this.setState({ showInUseModal: false });
              }}
              onCopy={() => {
                this.cloneNotebook();
              }}
            />
          )}
          {resolveRuntimeInitializer && (
            <RuntimeInitializerModal
              defaultRuntime={runtimeInitializerDefault}
              cancel={() => {
                closeRuntimeInitializerModal(null);
              }}
              createAndContinue={() => {
                closeRuntimeInitializerModal(runtimeInitializerDefault);
              }}
            />
          )}
        </div>
      );
    }

    private renderPreviewContents() {
      const { html, error } = this.state;
      if (error) {
        if (error instanceof ComputeSecuritySuspendedError) {
          return (
            <NotebookFrameError errorMode={ErrorMode.FORBIDDEN}>
              <SecuritySuspendedMessage error={error} />
            </NotebookFrameError>
          );
        }
        if (error instanceof RuntimeStatusError) {
          return (
            <NotebookFrameError errorMode={ErrorMode.ERROR}>
              {RUNTIME_ERROR_STATUS_MESSAGE_SHORT}
            </NotebookFrameError>
          );
        }
        const status = error instanceof Response ? error.status : 500;
        if (status === 412) {
          return (
            <NotebookFrameError errorMode={ErrorMode.INVALID}>
              Notebook is too large to display in preview mode, please use edit
              mode to view this notebook.
            </NotebookFrameError>
          );
        } else {
          return (
            <NotebookFrameError errorMode={ErrorMode.ERROR}>
              Failed to render preview due to an unknown error, please try
              reloading or opening the notebook in edit.
            </NotebookFrameError>
          );
        }
      }
      if (html) {
        return (
          <iframe
            id='notebook-frame'
            style={styles.previewFrame}
            srcDoc={html}
          />
        );
      }
      return <SpinnerOverlay />;
    }

    private startEditMode() {
      const { ns, terraName, nbName } = this.props.match.params;
      const {
        userAppsStore: { userApps },
        navigate,
      } = this.props;

      const { appType } = getAppInfoFromFileName(nbName);
      if (this.canStartRuntimes) {
        if (!this.notebookInUse) {
          if (appType === UIAppType.JUPYTER) {
            this.runRuntime(() => {
              this.navigateEditMode();
            });
          } else {
            openAppOrConfigPanel(ns, terraName, userApps, appType, navigate);
          }
        } else {
          this.setState({
            showInUseModal: true,
          });
        }
      }
    }
  }
);
