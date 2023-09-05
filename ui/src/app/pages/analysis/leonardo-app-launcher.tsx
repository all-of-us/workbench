import * as React from 'react';
import Iframe from 'react-iframe';
import { RouteComponentProps, withRouter } from 'react-router-dom';
import * as fp from 'lodash/fp';

import { Profile, Runtime, RuntimeStatus } from 'generated/fetch';

import { environment } from 'environments/environment';
import { switchCase } from '@terra-ui-packages/core-utils';
import { parseQueryParams } from 'app/components/app-router';
import { Button, StyledRouterLink } from 'app/components/buttons';
import { FlexRow } from 'app/components/flex';
import { ClrIcon } from 'app/components/icons';
import { RuntimeInitializerModal } from 'app/components/runtime-initializer-modal';
import { Spinner } from 'app/components/spinners';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import { NotebookIcon } from 'app/icons/notebook-icon';
import { ReminderIcon } from 'app/icons/reminder';
import {
  leoJupyterApi,
  leoProxyApi,
} from 'app/services/notebooks-swagger-fetch-clients';
import { runtimeApi } from 'app/services/swagger-fetch-clients';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import { reactStyles, withCurrentWorkspace, withUserProfile } from 'app/utils';
import { InitialRuntimeNotFoundError } from 'app/utils/leo-runtime-initializer';
import { NavigationProps } from 'app/utils/navigation';
import { Kernels } from 'app/utils/notebook-kernels';
import { fetchAbortableRetry } from 'app/utils/retry';
import {
  ComputeSecuritySuspendedError,
  maybeInitializeRuntime,
  RUNTIME_ERROR_STATUS_MESSAGE_SHORT,
  RuntimeStatusError,
  SparkConsolePath,
  withRuntimeStore,
} from 'app/utils/runtime-utils';
import { MatchParams, RuntimeStore } from 'app/utils/stores';
import { analysisTabName } from 'app/utils/user-apps-utils';
import { withNavigation } from 'app/utils/with-navigation-hoc';
import { WorkspaceData } from 'app/utils/workspace-data';

import {
  ErrorMode,
  NotebookFrameError,
  SecuritySuspendedMessage,
} from './notebook-frame-error';
import {
  appendJupyterNotebookFileSuffix,
  dropJupyterNotebookFileSuffix,
} from './util';

export enum LeoApplicationType {
  Notebook,
  Terminal,
  SparkConsole,
}

export enum Progress {
  Unknown,
  Initializing,
  Resuming,
  Authenticating,
  Copying,
  Creating,
  Redirecting,
  Loaded,
}

export const notebookProgressStrings: Map<Progress, string> = new Map([
  [Progress.Unknown, 'Connecting to the notebook server'],
  [
    Progress.Initializing,
    'Initializing notebook server, may take up to 5 minutes',
  ],
  [Progress.Resuming, 'Resuming notebook server, may take up to 1 minute'],
  [Progress.Authenticating, 'Authenticating with the notebook server'],
  [Progress.Copying, 'Copying the notebook onto the server'],
  [Progress.Creating, 'Creating the new notebook'],
  [Progress.Redirecting, 'Redirecting to the notebook server'],
]);

export const genericProgressStrings: Map<Progress, string> = new Map([
  [Progress.Unknown, 'Connecting to the environment'],
  [Progress.Initializing, 'Initializing environment, may take up to 5 minutes'],
  [Progress.Resuming, 'Resuming environment, may take up to 1 minute'],
  [Progress.Authenticating, 'Authenticating with the environment'],
  [Progress.Copying, 'Copying notebooks into the environment'],
  [Progress.Creating, 'Opening the application'],
  [Progress.Redirecting, 'Redirecting to the environment'],
]);

const getProgressString = (appType: LeoApplicationType, progress: Progress) => {
  if (appType === LeoApplicationType.Notebook) {
    return notebookProgressStrings.get(progress);
  } else {
    return genericProgressStrings.get(progress);
  }
};

// Statuses during which the user can interact with the Runtime UIs, e.g. via
// an iframe. When the runtime is in other states, requests to the runtime host
// are likely to fail.
const interactiveRuntimeStatuses = new Set<RuntimeStatus>([
  RuntimeStatus.Running,
  RuntimeStatus.Updating,
]);

const styles = reactStyles({
  main: {
    display: 'flex',
    flexDirection: 'column',
    marginLeft: '4.5rem',
    paddingTop: '1.5rem',
    width: '780px',
  },
  progressCard: {
    height: '180px',
    width: '195px',
    borderRadius: '5px',
    backgroundColor: colors.white,
    boxShadow: '0 0 2px 0 rgba(0,0,0,0.12), 0 3px 2px 0 rgba(0,0,0,0.12)',
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    padding: '1.5rem',
  },
  progressIcon: {
    height: '46px',
    width: '46px',
    marginBottom: '5px',
    fill: colorWithWhiteness(colors.primary, 0.9),
  },
  progressIconDone: {
    fill: colors.success,
  },
  progressText: {
    textAlign: 'center',
    color: colors.black,
    fontSize: 14,
    lineHeight: '22px',
    marginTop: '10px',
  },
  reminderText: {
    marginTop: '1.5rem',
    fontSize: 14,
    color: colors.primary,
  },
});

const commonNotebookFormat = {
  cells: [
    {
      cell_type: 'code',
      execution_count: null,
      metadata: {},
      outputs: [],
      source: [],
    },
  ],
  metadata: {},
  nbformat: 4,
  nbformat_minor: 2,
};

const rNotebookMetadata = {
  kernelspec: {
    display_name: 'R',
    language: 'R',
    name: 'ir',
  },
  language_info: {
    codemirror_mode: 'r',
    file_extension: '.r',
    mimetype: 'text/x-r-source',
    name: 'R',
    pygments_lexer: 'r',
    version: '3.4.4',
  },
};

const pyNotebookMetadata = {
  kernelspec: {
    display_name: 'Python 3',
    language: 'python',
    name: 'python3',
  },
  language_info: {
    codemirror_mode: {
      name: 'ipython',
      version: 3,
    },
    file_extension: '.py',
    mimetype: 'text/x-python',
    name: 'python',
    nbconvert_exporter: 'python',
    pygments_lexer: 'ipython3',
    version: '3.4.2',
  },
};

export enum ProgressCardState {
  UnknownInitializingResuming,
  Authenticating,
  CopyingCreating,
  Redirecting,
  Loaded,
}

interface Icon {
  shape: string;
  rotation?: string;
}

const progressCardIcons: Map<ProgressCardState, Icon> = new Map([
  [ProgressCardState.UnknownInitializingResuming, { shape: 'notebook' }],
  [ProgressCardState.Authenticating, { shape: 'success-standard' }],
  [ProgressCardState.CopyingCreating, { shape: 'copy' }],
  [
    ProgressCardState.Redirecting,
    { shape: 'circle-arrow', rotation: 'rotate(90deg)' },
  ],
]);

const progressCardStates: Map<ProgressCardState, Array<Progress>> = new Map([
  [
    ProgressCardState.UnknownInitializingResuming,
    [Progress.Unknown, Progress.Initializing, Progress.Resuming],
  ],
  [ProgressCardState.Authenticating, [Progress.Authenticating]],
  [ProgressCardState.CopyingCreating, [Progress.Creating, Progress.Copying]],
  [ProgressCardState.Redirecting, [Progress.Redirecting]],
]);

interface ProgressCardProps {
  progressState: Progress;
  cardState: ProgressCardState;
  progressComplete: Map<Progress, boolean>;
  creatingNewNotebook: boolean;
  leoAppType: LeoApplicationType;
}
const ProgressCard = ({
  progressState,
  cardState,
  progressComplete,
  creatingNewNotebook,
  leoAppType,
}: ProgressCardProps) => {
  const includesStates = progressCardStates.get(cardState);
  const isCurrent = includesStates.includes(progressState);
  const isComplete = includesStates.every(
    (s) => s.valueOf() < progressState.valueOf()
  );

  // Conditionally render card text
  const renderText = () => {
    switch (cardState) {
      case ProgressCardState.UnknownInitializingResuming:
        if (
          progressState === Progress.Unknown ||
          progressComplete[Progress.Unknown]
        ) {
          return getProgressString(leoAppType, Progress.Unknown);
        } else if (
          progressState === Progress.Initializing ||
          progressComplete[Progress.Initializing]
        ) {
          return getProgressString(leoAppType, Progress.Initializing);
        } else {
          return getProgressString(leoAppType, Progress.Resuming);
        }
      case ProgressCardState.Authenticating:
        return getProgressString(leoAppType, Progress.Authenticating);
      case ProgressCardState.CopyingCreating:
        if (creatingNewNotebook) {
          return getProgressString(leoAppType, Progress.Creating);
        } else {
          return getProgressString(leoAppType, Progress.Copying);
        }
      case ProgressCardState.Redirecting:
        return getProgressString(leoAppType, Progress.Redirecting);
    }
  };

  const icon = progressCardIcons.get(cardState);
  return (
    <div
      data-test-id={isCurrent ? 'current-progress-card' : ''}
      style={
        isCurrent
          ? { ...styles.progressCard, backgroundColor: '#F2FBE9' }
          : styles.progressCard
      }
    >
      {isCurrent ? (
        <Spinner
          style={{ width: '46px', height: '46px' }}
          data-test-id={'progress-card-spinner-' + cardState.valueOf()}
        />
      ) : (
        <React.Fragment>
          {icon.shape === 'notebook' ? (
            <NotebookIcon style={styles.progressIcon} />
          ) : (
            <ClrIcon
              shape={icon.shape}
              style={
                isComplete
                  ? {
                      ...styles.progressIcon,
                      ...styles.progressIconDone,
                      transform: icon.rotation,
                    }
                  : { ...styles.progressIcon, transform: icon.rotation }
              }
            />
          )}
        </React.Fragment>
      )}
      <div style={styles.progressText}>{renderText()}</div>
    </div>
  );
};

interface State {
  leoUrl: string;
  error: Error;
  runtimeInitializerDefault: Runtime;
  resolveRuntimeInitializer: (Runtime) => void;
  progress: Progress;
  progressComplete: Map<Progress, boolean>;
}

/** Terminal decides by Leo. Currently seems Leo support 1 and 2 as Terminal names. */
const terminalName = '1';

interface Props
  extends WithSpinnerOverlayProps,
    NavigationProps,
    RouteComponentProps<MatchParams> {
  workspace: WorkspaceData;
  profileState: { profile: Profile; reload: Function; updateCache: Function };
  runtimeStore: RuntimeStore;
  leoAppType: LeoApplicationType;
}

const runtimeApiRetryTimeoutMillis = 10000;
const runtimeApiRetryAttempts = 5;
const redirectMillis = 1000;

export const LeonardoAppLauncher = fp.flow(
  withUserProfile(),
  withCurrentWorkspace(),
  withRuntimeStore(),
  withNavigation,
  withRouter
)(
  class extends React.Component<Props, State> {
    private redirectTimer: NodeJS.Timeout;
    private pollAborter = new AbortController();

    constructor(props) {
      super(props);
      this.state = {
        leoUrl: undefined,
        error: props.runtimeStore.loadingError,
        runtimeInitializerDefault: null,
        resolveRuntimeInitializer: null,
        progress: Progress.Unknown,
        progressComplete: new Map<Progress, boolean>(),
      };
    }

    private isRuntimeInProgress(status: RuntimeStatus): boolean {
      return (
        status === RuntimeStatus.Starting ||
        status === RuntimeStatus.Stopping ||
        status === RuntimeStatus.Stopped
      );
    }

    private isCreatingNewNotebook() {
      const creating = parseQueryParams(this.props.location.search).get(
        'creating'
      );
      return !!creating;
    }

    private isOpeningTerminal() {
      return this.props.leoAppType === LeoApplicationType.Terminal;
    }

    private isPlaygroundMode() {
      const playgroundMode = parseQueryParams(this.props.location.search).get(
        'playgroundMode'
      );
      return playgroundMode === 'true';
    }

    private async runtimeRetry<T>(f: () => Promise<T>): Promise<T> {
      return await fetchAbortableRetry(
        f,
        runtimeApiRetryTimeoutMillis,
        runtimeApiRetryAttempts
      );
    }

    private getLeoAppUrl(runtime: Runtime, nbName: string): string {
      const proxyPath = switchCase(
        this.props.leoAppType,
        [LeoApplicationType.Notebook, () => `jupyter/notebooks/${nbName}`],
        [
          LeoApplicationType.Terminal,
          () => `jupyter/terminals/${terminalName}`,
        ],
        [LeoApplicationType.SparkConsole, () => this.getSparkConsolePath()]
      );

      return (
        environment.leoApiUrl +
        '/proxy/' +
        runtime.googleProject +
        '/' +
        runtime.runtimeName +
        '/' +
        proxyPath
      );
    }

    // get notebook name without file suffix
    private getNotebookName() {
      const { nbName } = this.props.match.params;
      // safe whether nbName has the standard notebook suffix or not
      return dropJupyterNotebookFileSuffix(decodeURIComponent(nbName));
    }

    private getSparkConsolePath(): string {
      const { sparkConsolePath } = this.props.match.params;
      if (
        !Object.values(SparkConsolePath).some((v) => v === sparkConsolePath)
      ) {
        throw Error(`invalid sparkConsolePath: '${sparkConsolePath}'`);
      }
      return sparkConsolePath;
    }

    private getPageTitle() {
      if (this.isOpeningTerminal()) {
        return 'Loading Terminal';
      } else if (this.props.leoAppType === LeoApplicationType.Notebook) {
        return this.isCreatingNewNotebook()
          ? 'Creating New Notebook: '
          : 'Loading Notebook: ' + this.getNotebookName();
      } else {
        return 'Loading Analysis Environment';
      }
    }

    // get notebook name with file suffix
    private getFullJupyterNotebookName() {
      return appendJupyterNotebookFileSuffix(this.getNotebookName());
    }

    private incrementProgress(p: Progress): void {
      this.setState((state) => ({
        progress: p,
        progressComplete: new Map(state.progressComplete).set(p, true),
      }));
    }

    componentDidMount() {
      this.props.hideSpinner();
    }

    componentDidUpdate(prevProps: Props) {
      const {
        runtimeStore: { runtime, runtimeLoaded, loadingError },
        workspace,
      } = this.props;

      if (loadingError && !prevProps.runtimeStore.loadingError) {
        this.setState({ error: loadingError });
      }

      // Only kick off the initialization process once the runtime is loaded.
      if (this.state.progress === Progress.Unknown && runtimeLoaded) {
        this.initializeRuntimeStatusChecking(workspace.namespace).catch(
          async (error) => this.setState({ error })
        );
      }

      // If we're already loaded (viewing the notebooks iframe), and the
      // runtime transitions out of an interactive state, navigate back to
      // the preview page as the iframe will start erroring.
      const isLoaded = runtimeLoaded && this.state.progress === Progress.Loaded;
      const { status: prevStatus = null } =
        prevProps.runtimeStore.runtime || {};
      const { status: curStatus = null } = runtime || {};
      if (
        isLoaded &&
        interactiveRuntimeStatuses.has(prevStatus) &&
        !interactiveRuntimeStatuses.has(curStatus)
      ) {
        this.props.navigate([
          'workspaces',
          workspace.namespace,
          workspace.id,
          // navigate will encode the notebook name automatically
          analysisTabName,
          ...(this.props.leoAppType === LeoApplicationType.Notebook
            ? ['preview', this.getFullJupyterNotebookName()]
            : []),
        ]);
      }
    }

    componentWillUnmount() {
      clearTimeout(this.redirectTimer);
      if (this.state.resolveRuntimeInitializer) {
        this.state.resolveRuntimeInitializer(null);
      }
      this.pollAborter.abort();
    }

    // check the runtime's status: if it's Running we can connect the notebook to it
    // otherwise we need to start polling
    private async initializeRuntimeStatusChecking(billingProjectId) {
      this.incrementProgress(Progress.Unknown);

      let { runtime } = this.props.runtimeStore;
      if (this.isRuntimeInProgress(runtime?.status)) {
        this.incrementProgress(Progress.Resuming);
      } else {
        this.incrementProgress(Progress.Initializing);
      }

      try {
        runtime = await maybeInitializeRuntime(
          billingProjectId,
          this.pollAborter.signal
        );
      } catch (e) {
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
          if (!runtimeToCreate) {
            throw e;
          }
          runtime = await maybeInitializeRuntime(
            billingProjectId,
            this.pollAborter.signal,
            runtimeToCreate
          );
        } else {
          throw e;
        }
      }
      await this.connectToRunningRuntime(runtime);
    }

    private async connectToRunningRuntime(runtime: Runtime) {
      const { namespace, id } = this.props.workspace;
      this.incrementProgress(Progress.Authenticating);

      const leoAppLocation = await this.getLeoAppPathAndLocalize(runtime);
      if (this.isCreatingNewNotebook()) {
        window.history.replaceState(
          {},
          'Notebook',
          `workspaces/${namespace}/${id}/${analysisTabName}/${encodeURIComponent(
            this.getFullJupyterNotebookName()
          )}`
        );
      }
      if (this.isOpeningTerminal()) {
        leoProxyApi().connectToTerminal(
          runtime.googleProject,
          runtime.runtimeName,
          terminalName
        );
      }
      this.setState({ leoUrl: this.getLeoAppUrl(runtime, leoAppLocation) });
      this.incrementProgress(Progress.Redirecting);

      // give it a second to "redirect"
      this.redirectTimer = global.setTimeout(
        () => this.incrementProgress(Progress.Loaded),
        redirectMillis
      );
    }

    private async getLeoAppPathAndLocalize(runtime: Runtime) {
      if (this.props.leoAppType === LeoApplicationType.Notebook) {
        if (this.isCreatingNewNotebook()) {
          this.incrementProgress(Progress.Creating);
          return this.createNotebookAndLocalize(runtime);
        } else {
          this.incrementProgress(Progress.Copying);
          const fullNotebookName = this.getFullJupyterNotebookName();
          const localizedNotebookDir = await this.localizeNotebooks([
            fullNotebookName,
          ]);
          return `${localizedNotebookDir}/${fullNotebookName}`;
        }
      }

      const { workspace } = this.props;
      this.incrementProgress(Progress.Creating);
      const resp = await this.runtimeRetry(() =>
        runtimeApi().localize(workspace.namespace, null, {
          signal: this.pollAborter.signal,
        })
      );
      return resp.runtimeLocalDirectory;
    }

    private async createNotebookAndLocalize(runtime: Runtime) {
      const fileContent = commonNotebookFormat;
      const kernelType = parseQueryParams(this.props.location.search).get(
        'kernelType'
      );
      if (kernelType === Kernels.R.toString()) {
        fileContent.metadata = rNotebookMetadata;
      } else {
        fileContent.metadata = pyNotebookMetadata;
      }
      const localizedDir = await this.localizeNotebooks([]);
      // Use the Jupyter Server API directly to create a new notebook. This
      // API handles notebook name collisions and matches the behavior of
      // clicking 'new notebook' in the Jupyter UI.
      const workspaceDir = localizedDir.replace(/^workspaces\//, '');
      const jupyterResp = await this.runtimeRetry(() =>
        leoJupyterApi().putContents(
          runtime.googleProject,
          runtime.runtimeName,
          workspaceDir,
          this.getFullJupyterNotebookName(),
          {
            type: 'file',
            format: 'text',
            content: JSON.stringify(fileContent),
          },
          { signal: this.pollAborter.signal }
        )
      );
      return `${localizedDir}/${jupyterResp.name}`;
    }

    private async localizeNotebooks(notebookNames: Array<string>) {
      const { workspace } = this.props;
      const resp = await this.runtimeRetry(() =>
        runtimeApi().localize(
          workspace.namespace,
          {
            notebookNames,
            playgroundMode: this.isPlaygroundMode(),
          },
          { signal: this.pollAborter.signal }
        )
      );
      return resp.runtimeLocalDirectory;
    }

    render() {
      const {
        error,
        progress,
        runtimeInitializerDefault,
        resolveRuntimeInitializer,
        progressComplete,
        leoUrl,
      } = this.state;
      const { leoAppType } = this.props;
      const creatingNewNotebook = this.isCreatingNewNotebook();

      const closeRuntimeInitializerModal = (r?: Runtime) => {
        resolveRuntimeInitializer(r);
        this.setState({
          runtimeInitializerDefault: null,
          resolveRuntimeInitializer: null,
        });
      };

      if (error) {
        if (error instanceof ComputeSecuritySuspendedError) {
          return (
            <NotebookFrameError errorMode={ErrorMode.FORBIDDEN}>
              <SecuritySuspendedMessage error={error} />
            </NotebookFrameError>
          );
        } else if (error instanceof RuntimeStatusError) {
          return (
            <NotebookFrameError errorMode={ErrorMode.ERROR}>
              {RUNTIME_ERROR_STATUS_MESSAGE_SHORT}
            </NotebookFrameError>
          );
        } else if (error instanceof InitialRuntimeNotFoundError) {
          return (
            <NotebookFrameError errorMode={ErrorMode.INVALID}>
              This action requires an analysis environment. Please configure
              your environment via the cloud analysis panel and refresh to try
              again.
            </NotebookFrameError>
          );
        } else {
          return (
            <NotebookFrameError>
              <>Unknown error loading analysis environment, please try again.</>
            </NotebookFrameError>
          );
        }
      }
      return (
        <React.Fragment>
          {progress !== Progress.Loaded ? (
            <div style={styles.main}>
              <div
                style={{
                  display: 'flex',
                  flexDirection: 'row',
                  justifyContent: 'space-between',
                }}
                data-test-id='leo-app-launcher'
              >
                <h2 style={{ lineHeight: 0 }}>{this.getPageTitle()}</h2>
                <Button type='secondary' onClick={() => window.history.back()}>
                  Cancel
                </Button>
              </div>
              <div
                style={{
                  display: 'flex',
                  flexDirection: 'row',
                  marginTop: '1.5rem',
                }}
              >
                {Array.from(progressCardStates, ([key, _], index) => {
                  return (
                    <ProgressCard
                      key={index}
                      progressState={progress}
                      cardState={key}
                      creatingNewNotebook={creatingNewNotebook}
                      progressComplete={progressComplete}
                      leoAppType={leoAppType}
                    />
                  );
                })}
              </div>
              <FlexRow style={styles.reminderText}>
                <ReminderIcon
                  style={{
                    width: '2.625rem',
                    height: '2.625rem',
                    marginRight: '0.75rem',
                  }}
                />
                <div>
                  You are prohibited from taking screenshots or attempting in
                  any way to remove participant-level data from the workbench.
                </div>
              </FlexRow>
              <div style={{ marginLeft: '3rem', ...styles.reminderText }}>
                You are also prohibited from publishing or otherwise
                distributing any data or aggregate statistics corresponding to
                fewer than 20 participants unless expressly permitted by our
                data use policies.
              </div>
              <div style={{ marginLeft: '3rem', ...styles.reminderText }}>
                For more information, please see our{' '}
                <StyledRouterLink path='/data-code-of-conduct'>
                  Data Use Policies.
                </StyledRouterLink>
              </div>
            </div>
          ) : (
            <div style={{ height: '100%' }}>
              <div
                style={{ borderBottom: '5px solid #2691D0', width: '100%' }}
              />
              <Iframe frameBorder={0} url={leoUrl} width='100%' height='100%' />
            </div>
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
        </React.Fragment>
      );
    }
  }
);
