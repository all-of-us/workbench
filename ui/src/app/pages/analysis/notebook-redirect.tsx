import {Component} from '@angular/core';
import * as fp from 'lodash/fp';
import * as React from 'react';
import Iframe from 'react-iframe';

import {urlParamsStore} from 'app/utils/navigation';

import {Button} from 'app/components/buttons';
import {ClrIcon} from 'app/components/icons';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {Spinner} from 'app/components/spinners';
import {NotebookIcon} from 'app/icons/notebook-icon';
import {ReminderIcon} from 'app/icons/reminder';
import {jupyterApi, notebooksApi, notebooksClusterApi} from 'app/services/notebooks-swagger-fetch-clients';
import {clusterApi} from 'app/services/swagger-fetch-clients';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {
  reactStyles,
  ReactWrapperBase,
  withCurrentWorkspace,
  withQueryParams,
  withUserProfile
} from 'app/utils';
import {Kernels} from 'app/utils/notebook-kernels';
import {WorkspaceData} from 'app/utils/workspace-data';
import {environment} from 'environments/environment';
import {Cluster, ClusterStatus, Profile} from 'generated/fetch';
import {appendNotebookFileSuffix, dropNotebookFileSuffix} from './util';

enum Progress {
  Unknown,
  Initializing,
  Resuming,
  Authenticating,
  Copying,
  Creating,
  Redirecting,
  Loaded
}

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
    display: 'flex', flexDirection: 'row', marginTop: '1rem', fontSize: 14, color: colors.primary
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

const ProgressCard: React.FunctionComponent<{currentState: Progress, index: ProgressCardState,
  progressComplete: Map<Progress, boolean>, creatingNewNotebook: boolean}> =
  ({currentState, index, progressComplete, creatingNewNotebook}) => {
    const includesStates = progressCardStates.get(index);
    const isCurrent = includesStates.includes(currentState);
    const isComplete = includesStates.every(s => s.valueOf() < currentState.valueOf());

    // Conditionally render card text
    const renderText = () => {
      switch (index) {
        case ProgressCardState.UnknownInitializingResuming:
          if (currentState === Progress.Unknown || progressComplete[Progress.Unknown]) {
            return 'Connecting to the notebook server';
          } else if (currentState === Progress.Initializing ||
            progressComplete[Progress.Initializing]) {
            return 'Initializing notebook server, may take up to 10 minutes';
          } else {
            return 'Resuming notebook server, may take up to 1 minute';
          }
        case ProgressCardState.Authenticating:
          return 'Authenticating with the notebook server';
        case ProgressCardState.CopyingCreating:
          if (creatingNewNotebook) {
            return 'Creating the new notebook';
          } else {
            return 'Copying the notebook onto the server';
          }
        case ProgressCardState.Redirecting:
          return 'Redirecting to the notebook server';
      }
    };

    const icon = progressCardIcons.get(index);
    return <div style={isCurrent ? {...styles.progressCard, backgroundColor: '#F2FBE9'} :
      styles.progressCard}>
      {isCurrent ? <Spinner style={{width: '46px', height: '46px'}}
                            data-test-id={'progress-card-spinner-' + index.valueOf()}/> :
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
  initialized: boolean;
  leoUrl: string;
  localizationError: boolean;
  progress: Progress;
  progressComplete: Map<Progress, boolean>;
}

interface Props {
  workspace: WorkspaceData;
  queryParams: any;
  profileState: {profile: Profile, reload: Function, updateCache: Function};
}

export const NotebookRedirect = fp.flow(withUserProfile(), withCurrentWorkspace(),
  withQueryParams())(class extends React.Component<Props, State> {

    private timeoutReference: NodeJS.Timer;

    constructor(props) {
      super(props);
      this.state = {
        initialized: false,
        leoUrl: undefined,
        localizationError: false,
        progress: Progress.Unknown,
        progressComplete: new Map<Progress, boolean>(),
      };
    }

    componentDidMount() {
      this.pollCluster(this.props.workspace.namespace);
    }

    componentWillUnmount() {
      clearTimeout(this.timeoutReference);
    }

    private isClusterInProgress(cluster: Cluster): boolean {
      return cluster.status === ClusterStatus.Starting ||
        cluster.status === ClusterStatus.Stopping ||
        cluster.status === ClusterStatus.Stopped;
    }

    private clearAndSetTimeout(timeoutCallback, timeoutMillis) {
      clearTimeout(this.timeoutReference);
      this.timeoutReference = setTimeout(() => timeoutCallback(), timeoutMillis);
    }

    private isCreatingNewNotebook() {
      return !!this.props.queryParams.creating;
    }

    private isPlaygroundMode() {
      return this.props.queryParams.playgroundMode === 'true';
    }

    private async pollCluster(billingProjectId) {
      const repoll = () => {
        this.clearAndSetTimeout(() => this.pollCluster(billingProjectId), 15000);
      };
      const {workspace} = this.props;
      const {initialized} = this.state;
      try {
        const resp = await clusterApi().listClusters(billingProjectId, workspace.id);
        const cluster = resp.defaultCluster;
        if (!initialized) {
          if (cluster.status === ClusterStatus.Running) {
            this.incrementProgress(Progress.Unknown);
          } else if (this.isClusterInProgress(cluster)) {
            this.incrementProgress(Progress.Resuming);
          } else {
            this.incrementProgress(Progress.Initializing);
          }
          this.setState({initialized: true});
        }

        if (cluster.status === ClusterStatus.Running) {
          this.incrementProgress(Progress.Authenticating);
          await this.initializeNotebookCookies(cluster);

          const notebookLocation = await this.getNotebookPathAndLocalize(cluster);
          if (this.isCreatingNewNotebook()) {
            window.history.replaceState({}, 'Notebook', 'workspaces/' + workspace.namespace
              + '/' + workspace.id + '/notebooks/' +
              encodeURIComponent(this.getFullNotebookName()));
          }
          this.setState({leoUrl: this.notebookUrl(cluster, notebookLocation)});
          this.incrementProgress(Progress.Redirecting);

          // give it a second to "redirect"
          this.clearAndSetTimeout(() => this.incrementProgress(Progress.Loaded), 1000);
        } else {
          // If cluster is not running, keep re-polling until it is.
          if (cluster.status === ClusterStatus.Stopped) {
            await notebooksClusterApi().startCluster(cluster.clusterNamespace, cluster.clusterName);
          }
          repoll();
        }
      } catch (e) {
        repoll();
      }
    }

    private incrementProgress(p: Progress): void {
      this.setState({
        progress: p,
        progressComplete:  this.state.progressComplete.set(p, true)
      });
    }

    private notebookUrl(cluster: Cluster, nbName: string): string {
      return encodeURI(
        environment.leoApiUrl + '/notebooks/'
        + cluster.clusterNamespace + '/'
        + cluster.clusterName + '/notebooks/' + nbName);
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

    private async initializeNotebookCookies(c: Cluster) {
      return notebooksApi().setCookie(c.clusterNamespace, c.clusterName,
        {
          withCredentials: true,
          crossDomain: true,
          credentials: 'include'
        });
    }

    private async getNotebookPathAndLocalize(cluster: Cluster) {
      const fullNotebookName = this.getFullNotebookName();
      if (this.isCreatingNewNotebook()) {
        this.incrementProgress(Progress.Creating);
        return this.createNotebookAndLocalize(cluster);
      } else {
        this.incrementProgress(Progress.Copying);
        const localizedNotebookDir =
          await this.localizeNotebooksWithRetry(cluster, [fullNotebookName]);
        return `${localizedNotebookDir}/${fullNotebookName}`;
      }
    }

    private async localizeNotebooksWithRetry(cluster: Cluster, notebookNames: Array<string>, retryCount: number = 0) {
      const {workspace} = this.props;
      try {
        const resp = await clusterApi().localize(cluster.clusterNamespace, cluster.clusterName,
          {workspaceNamespace: workspace.namespace, workspaceId: workspace.id,
            notebookNames: notebookNames, playgroundMode: this.isPlaygroundMode()});
        return resp.clusterLocalDirectory;
      } catch (error) {
        retryCount += 1;
        if (retryCount <= 3) {
          console.error('retrying notebook localization');
          return this.localizeNotebooksWithRetry(cluster, notebookNames, retryCount);
        } else {
          console.error(error);
          this.setState({localizationError: true});
        }
      }
    }

    private async createNotebookAndLocalize(cluster: Cluster) {
      const fileContent = commonNotebookFormat;
      const {kernelType} = this.props.queryParams;
      if (kernelType === Kernels.R.toString()) {
        fileContent.metadata = rNotebookMetadata;
      } else {
        fileContent.metadata = pyNotebookMetadata;
      }
      const localizedDir = await this.localizeNotebooksWithRetry(cluster, []);
      // Use the Jupyter Server API directly to create a new notebook. This
      // API handles notebook name collisions and matches the behavior of
      // clicking 'new notebook' in the Jupyter UI.
      const workspaceDir = localizedDir.replace(/^workspaces\//, '');
      const jupyterResp = await jupyterApi().putContents(
        cluster.clusterNamespace, cluster.clusterName, workspaceDir, this.getFullNotebookName(), {
          'type': 'file',
          'format': 'text',
          'content': JSON.stringify(fileContent)
        }
      );
      return `${localizedDir}/${jupyterResp.name}`;
    }

    render() {
      const {localizationError, progress, progressComplete, leoUrl} = this.state;
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
            {Array.from(progressCardStates, ([key, _]) => {
              return <ProgressCard currentState={progress} index={key}
                                   creatingNewNotebook={creatingNewNotebook} progressComplete={progressComplete}/>;
            })}
          </div>
          <div style={styles.reminderText}>
            <ReminderIcon
              style={{height: '80px', width: '80px', marginRight: '0.5rem'}}/>
            It is All of Us data use policy that researchers should not make copies of
            or download individual-level data (including taking screenshots or other means
            of viewing individual-level data) outside of the All of Us research environment
            without approval from All of Us Resource Access Board (RAB).
          </div>
        </div> : <div style={{height: '100%'}}>
          <div style={{borderBottom: '5px solid #2691D0', width: '100%'}}/>
          <Iframe frameBorder={0} url={leoUrl} width='100%' height='100%'/>
        </div>}
        {localizationError && <Modal>
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

@Component({
  template: '<div #root style="height: 100%"></div>'
})
export class NotebookRedirectComponent extends ReactWrapperBase {
  constructor() {
    super(NotebookRedirect, []);
  }
}

