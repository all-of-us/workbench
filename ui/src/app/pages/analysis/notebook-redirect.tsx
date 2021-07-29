import * as fp from 'lodash/fp';
import * as React from 'react';
import Iframe from 'react-iframe';

import {NavigationProps, urlParamsStore} from 'app/utils/navigation';
import {fetchAbortableRetry} from 'app/utils/retry';
import {RuntimeStore} from 'app/utils/stores';

import {Button} from 'app/components/buttons';
import {FlexRow} from 'app/components/flex';
import {ClrIcon} from 'app/components/icons';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {Spinner} from 'app/components/spinners';
import {WithSpinnerOverlayProps} from 'app/components/with-spinner-overlay';
import {NotebookIcon} from 'app/icons/notebook-icon';
import {ReminderIcon} from 'app/icons/reminder';
import {jupyterApi, proxyApi} from 'app/services/notebooks-swagger-fetch-clients';
import {runtimeApi} from 'app/services/swagger-fetch-clients';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {
  reactStyles,
  withCurrentWorkspace, withQueryParams,
  withUserProfile
} from 'app/utils';
import {Kernels} from 'app/utils/notebook-kernels';
import {maybeInitializeRuntime, withRuntimeStore} from 'app/utils/runtime-utils';
import {WorkspaceData} from 'app/utils/workspace-data';
import {environment} from 'environments/environment';
import {Profile, Runtime, RuntimeStatus} from 'generated/fetch';
import {withNavigation} from '../../utils/navigation-wrapper';
import {appendNotebookFileSuffix, dropNotebookFileSuffix} from './util';

export enum Progress {
  Unknown,
  Initializing,
  Resuming,
  Authenticating,
  Copying,
  Creating,
  Redirecting,
  Loaded
}

export const progressStrings: Map<Progress, string> = new Map([
  [Progress.Unknown, 'Connecting to the notebook server'],
  [Progress.Initializing, 'Initializing notebook server, may take up to 5 minutes'],
  [Progress.Resuming, 'Resuming notebook server, may take up to 1 minute'],
  [Progress.Authenticating, 'Authenticating with the notebook server'],
  [Progress.Copying, 'Copying the notebook onto the server'],
  [Progress.Creating, 'Creating the new notebook'],
  [Progress.Redirecting, 'Redirecting to the notebook server'],
]);

const styles = reactStyles({
  main: {
    display: 'flex', flexDirection: 'column', marginLeft: '3rem', paddingTop: '1rem', width: '780px'
  },
  progressCard: {
    height: '180px', width: '195px', borderRadius: '5px', backgroundColor: colors.white,
    boxShadow: '0 0 2px 0 rgba(0,0,0,0.12), 0 3px 2px 0 rgba(0,0,0,0.12)', display: 'flex',
    flexDirection: 'column', alignItems: 'center', padding: '1rem'
  },
  progressIcon: {
    height: '46px', width: '46px', marginBottom: '5px',
    fill: colorWithWhiteness(colors.primary, 0.9)
  },
  progressIconDone: {
    fill: colors.success
  },
  progressText: {
    textAlign: 'center', color: colors.black, fontSize: 14, lineHeight: '22px', marginTop: '10px'
  },
  reminderText: {
    marginTop: '1rem', fontSize: 14, color: colors.primary
  }
});


const commonNotebookFormat = {
  'cells': [
    {
      'cell_type': 'code',
      'execution_count': null,
      'metadata': {},
      'outputs': [],
      'source': []
    }
  ],
  metadata: {},
  'nbformat': 4,
  'nbformat_minor': 2
};

const rNotebookMetadata = {
  'kernelspec': {
    'display_name': 'R',
    'language': 'R',
    'name': 'ir'
  },
  'language_info': {
    'codemirror_mode': 'r',
    'file_extension': '.r',
    'mimetype': 'text/x-r-source',
    'name': 'R',
    'pygments_lexer': 'r',
    'version': '3.4.4'
  }
};

const pyNotebookMetadata = {
  'kernelspec': {
    'display_name': 'Python 3',
    'language': 'python',
    'name': 'python3'
  },
  'language_info': {
    'codemirror_mode': {
      'name': 'ipython',
      'version': 3
    },
    'file_extension': '.py',
    'mimetype': 'text/x-python',
    'name': 'python',
    'nbconvert_exporter': 'python',
    'pygments_lexer': 'ipython3',
    'version': '3.4.2'
  }
};

export enum ProgressCardState {
  UnknownInitializingResuming,
  Authenticating,
  CopyingCreating,
  Redirecting,
  Loaded
}

interface Icon {
  shape: string;
  rotation?: string;
}

const progressCardIcons: Map<ProgressCardState, Icon> = new Map([
  [ProgressCardState.UnknownInitializingResuming, {shape: 'notebook'}],
  [ProgressCardState.Authenticating, {shape: 'success-standard'}],
  [ProgressCardState.CopyingCreating, {shape: 'copy'}],
  [ProgressCardState.Redirecting, {shape: 'circle-arrow', rotation: 'rotate(90deg)'}],
]);

const progressCardStates: Map<ProgressCardState, Array<Progress>> = new Map([
  [ProgressCardState.UnknownInitializingResuming, [Progress.Unknown, Progress.Initializing, Progress.Resuming]],
  [ProgressCardState.Authenticating, [Progress.Authenticating]],
  [ProgressCardState.CopyingCreating, [Progress.Creating, Progress.Copying]],
  [ProgressCardState.Redirecting, [Progress.Redirecting]]
]);

const ProgressCard: React.FunctionComponent<{progressState: Progress, cardState: ProgressCardState,
  progressComplete: Map<Progress, boolean>, creatingNewNotebook: boolean}> =
  ({progressState, cardState, progressComplete, creatingNewNotebook}) => {
    const includesStates = progressCardStates.get(cardState);
    const isCurrent = includesStates.includes(progressState);
    const isComplete = includesStates.every(s => s.valueOf() < progressState.valueOf());

    // Conditionally render card text
    const renderText = () => {
      switch (cardState) {
        case ProgressCardState.UnknownInitializingResuming:
          if (progressState === Progress.Unknown || progressComplete[Progress.Unknown]) {
            return progressStrings.get(Progress.Unknown);
          } else if (progressState === Progress.Initializing ||
            progressComplete[Progress.Initializing]) {
            return progressStrings.get(Progress.Initializing);
          } else {
            return progressStrings.get(Progress.Resuming);
          }
        case ProgressCardState.Authenticating:
          return progressStrings.get(Progress.Authenticating);
        case ProgressCardState.CopyingCreating:
          if (creatingNewNotebook) {
            return progressStrings.get(Progress.Creating);
          } else {
            return progressStrings.get(Progress.Copying);
          }
        case ProgressCardState.Redirecting:
          return progressStrings.get(Progress.Redirecting);
      }
    };

    const icon = progressCardIcons.get(cardState);
    return <div data-test-id={isCurrent ? 'current-progress-card' : ''}
                style={isCurrent ? {...styles.progressCard, backgroundColor: '#F2FBE9'} :
      styles.progressCard}>
      {isCurrent ? <Spinner style={{width: '46px', height: '46px'}}
                            data-test-id={'progress-card-spinner-' + cardState.valueOf()}/> :
        <React.Fragment>
          {icon.shape === 'notebook' ? <NotebookIcon style={styles.progressIcon}/> :
          <ClrIcon shape={icon.shape} style={isComplete ?
          {...styles.progressIcon, ...styles.progressIconDone,
            transform: icon.rotation} :
            {...styles.progressIcon, transform: icon.rotation}}/>}
        </React.Fragment>}
        <div style={styles.progressText}>
          {renderText()}
        </div>
    </div>;
  };

interface State {
  leoUrl: string;
  showErrorModal: boolean;
  progress: Progress;
  progressComplete: Map<Progress, boolean>;
}

interface Props extends WithSpinnerOverlayProps, NavigationProps {
  workspace: WorkspaceData;
  queryParams: any;
  profileState: {profile: Profile, reload: Function, updateCache: Function};
  runtimeStore: RuntimeStore;
}

const runtimeApiRetryTimeoutMillis = 10000;
const runtimeApiRetryAttempts = 5;
const redirectMillis = 1000;

export const NotebookRedirect = fp.flow(
  withUserProfile(),
  withCurrentWorkspace(),
  withRuntimeStore(),
  withQueryParams(),
  withNavigation
)(
  class extends React.Component<Props, State> {

    private redirectTimer: NodeJS.Timeout;
    private pollAborter = new AbortController();

    constructor(props) {
      super(props);
      this.state = {
        leoUrl: undefined,
        showErrorModal: false,
        progress: Progress.Unknown,
        progressComplete: new Map<Progress, boolean>(),
      };
    }

    private isRuntimeInProgress(status: RuntimeStatus): boolean {
      return status === RuntimeStatus.Starting ||
          status === RuntimeStatus.Stopping ||
          status === RuntimeStatus.Stopped;
    }

    private isCreatingNewNotebook() {
      return !!this.props.queryParams.creating;
    }

    private isPlaygroundMode() {
      return this.props.queryParams.playgroundMode === 'true';
    }

    private async runtimeRetry<T>(f: () => Promise<T>): Promise<T> {
      return await fetchAbortableRetry(f, runtimeApiRetryTimeoutMillis, runtimeApiRetryAttempts);
    }

    private notebookUrl(runtime: Runtime, nbName: string): string {
      return encodeURI(
        environment.leoApiUrl + '/proxy/'
          + runtime.googleProject + '/'
          + runtime.runtimeName + '/jupyter/notebooks/' + nbName);
    }

    // get notebook name without file suffix
    private getNotebookName() {
      const {nbName} = urlParamsStore.getValue();
      // safe whether nbName has the standard notebook suffix or not
      return dropNotebookFileSuffix(decodeURIComponent(nbName));
    }

    // get notebook name with file suffix
    private getFullNotebookName() {
      return appendNotebookFileSuffix(this.getNotebookName());
    }

    private async initializeNotebookCookies(c: Runtime) {
      return await this.runtimeRetry(() => proxyApi().setCookie(c.googleProject, c.runtimeName, {
        withCredentials: true,
        crossDomain: true,
        credentials: 'include',
        signal: this.pollAborter.signal
      }));
    }

    private incrementProgress(p: Progress): void {
      this.setState((state) => ({
        progress: p,
        progressComplete: new Map(state.progressComplete).set(p, true)
      }));
    }

    componentDidMount() {
      this.props.hideSpinner();
    }

    componentDidUpdate(prevProps: Props) {
      const {runtimeStore: {runtime}, workspace} = this.props;

      // Only kick off the initialization process once the runtime is loaded.
      if (this.state.progress === Progress.Unknown && runtime !== undefined) {
        this.initializeRuntimeStatusChecking(workspace.namespace);
      }

      // If we're already loaded (viewing the notebooks iframe), and the
      // runtime transitions out of the "running" state, navigate back to
      // the preview page as the iframe will start erroring.
      const {status: prevStatus = null} = prevProps.runtimeStore.runtime || {};
      const isLoaded = this.state.progress === Progress.Loaded;
      if (isLoaded && prevStatus === RuntimeStatus.Running &&
          runtime !== undefined && (runtime === null || runtime.status !== RuntimeStatus.Running)) {
        this.props.navigate([
          'workspaces', workspace.namespace, workspace.id,
          // navigate will encode the notebook name automatically
          'notebooks', 'preview', this.getFullNotebookName()
        ]);
      }
    }

    componentWillUnmount() {
      clearTimeout(this.redirectTimer);
    }

    // check the runtime's status: if it's Running we can connect the notebook to it
    // otherwise we need to start polling
    private async initializeRuntimeStatusChecking(billingProjectId) {
      this.incrementProgress(Progress.Unknown);

      let {runtime} = this.props.runtimeStore;
      if (this.isRuntimeInProgress(runtime && runtime.status)) {
        this.incrementProgress(Progress.Resuming);
      } else {
        this.incrementProgress(Progress.Initializing);
      }

      runtime = await maybeInitializeRuntime(billingProjectId, this.pollAborter.signal);
      await this.connectToRunningRuntime(runtime);
    }

    private async connectToRunningRuntime(runtime: Runtime) {
      const {namespace, id} = this.props.workspace;
      this.incrementProgress(Progress.Authenticating);
      await this.initializeNotebookCookies(runtime);

      const notebookLocation = await this.getNotebookPathAndLocalize(runtime);
      if (this.isCreatingNewNotebook()) {
        window.history.replaceState({}, 'Notebook', 'workspaces/' + namespace
            + '/' + id + '/notebooks/' +
            encodeURIComponent(this.getFullNotebookName()));
      }
      this.setState({leoUrl: this.notebookUrl(runtime, notebookLocation)});
      this.incrementProgress(Progress.Redirecting);

      // give it a second to "redirect"
      this.redirectTimer = global.setTimeout(() => this.incrementProgress(Progress.Loaded), redirectMillis);
    }

    private async getNotebookPathAndLocalize(runtime: Runtime) {
      if (this.isCreatingNewNotebook()) {
        this.incrementProgress(Progress.Creating);
        return this.createNotebookAndLocalize(runtime);
      } else {
        this.incrementProgress(Progress.Copying);
        const fullNotebookName = this.getFullNotebookName();
        const localizedNotebookDir =
            await this.localizeNotebooks([fullNotebookName]);
        return `${localizedNotebookDir}/${fullNotebookName}`;
      }
    }

    private async createNotebookAndLocalize(runtime: Runtime) {
      const fileContent = commonNotebookFormat;
      const {kernelType} = this.props.queryParams;
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
      const jupyterResp = await this.runtimeRetry(() => jupyterApi().putContents(
        runtime.googleProject, runtime.runtimeName, workspaceDir, this.getFullNotebookName(), {
          'type': 'file',
          'format': 'text',
          'content': JSON.stringify(fileContent)
        },
        {signal: this.pollAborter.signal}));
      return `${localizedDir}/${jupyterResp.name}`;
    }

    private async localizeNotebooks(notebookNames: Array<string>) {
      const {workspace} = this.props;
      const resp = await this.runtimeRetry(() => runtimeApi().localize(
        workspace.namespace, {
          notebookNames, playgroundMode: this.isPlaygroundMode()
        },
        {signal: this.pollAborter.signal}));
      return resp.runtimeLocalDirectory;
    }

    render() {
      const {showErrorModal, progress, progressComplete, leoUrl} = this.state;
      const creatingNewNotebook = this.isCreatingNewNotebook();
      return <React.Fragment>
        {progress !== Progress.Loaded ? <div style={styles.main}>
          <div style={{display: 'flex', flexDirection: 'row', justifyContent: 'space-between'}}
               data-test-id='notebook-redirect'>
            <h2 style={{lineHeight: 0}}>
              {creatingNewNotebook ? 'Creating New Notebook: ' : 'Loading Notebook: '}
              {this.getNotebookName()}
            </h2>
            <Button type='secondary' onClick={() => window.history.back()}>Cancel</Button>
          </div>
          <div style={{display: 'flex', flexDirection: 'row', marginTop: '1rem'}}>
            {Array.from(progressCardStates, ([key, _], index) => {
              return <ProgressCard key={index} progressState={progress} cardState={key}
                                   creatingNewNotebook={creatingNewNotebook} progressComplete={progressComplete}/>;
            })}
          </div>
          <FlexRow style={styles.reminderText}>
            <ReminderIcon
              style={{width: '1.75rem', height: '1.75rem', marginRight: '0.5rem'}}/>
            <div>
              You are prohibited from taking screenshots or attempting in any way to remove participant-level data from the workbench.
            </div>
          </FlexRow>
          <div style={{marginLeft: '2rem', ...styles.reminderText}}>
            You are also prohibited from publishing or otherwise distributing any data or aggregate statistics corresponding to fewer than
            20 participants unless expressly permitted by our data use policies.
          </div>
          <div style={{marginLeft: '2rem', ...styles.reminderText}}>
            For more information, please see our  <a href={'/data-code-of-conduct'}>Data Use Policies.</a>
          </div>
        </div> : <div style={{height: '100%'}}>
          <div style={{borderBottom: '5px solid #2691D0', width: '100%'}}/>
          <Iframe frameBorder={0} url={leoUrl} width='100%' height='100%'/>
        </div>}
        {showErrorModal && <Modal>
          <ModalTitle>
            {creatingNewNotebook ? 'Error creating notebook.' : 'Error fetching notebook'}
          </ModalTitle>
          <ModalBody>
            Please refresh and try again.
          </ModalBody>
          <ModalFooter>
            <Button type='secondary' onClick={() => window.history.back()}>Go Back</Button>
          </ModalFooter>
        </Modal>}
      </React.Fragment>;
    }
  });

