import * as React from 'react';

import {Button} from 'app/components/buttons';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {TooltipTrigger} from 'app/components/popups';
import {clusterApi} from 'app/services/swagger-fetch-clients';
import {
  ClusterInitializationAbortedError,
  ClusterInitializationFailedError,
  ClusterInitializer,
} from 'app/utils/cluster-initializer';
import {
  ClusterStatus,
} from 'generated/fetch/api';
import {reportError} from 'app/utils/errors';

const RESTART_LABEL = 'Reset server';
const CREATE_LABEL = 'Create server';

const styles = {
  notebookSettings: {
    marginTop: '1rem'
  },
};

export interface Props {
  workspaceNamespace: string;
}

interface State {
  clusterStatus?: ClusterStatus;
  isPollingCluster: boolean;
  resetClusterPending: boolean;
  resetClusterModal: boolean;
  resetClusterFailure: boolean;
}

export class ResetClusterButton extends React.Component<Props, State> {
  private aborter = new AbortController();
  private initializer?: ClusterInitializer;

  constructor(props) {
    super(props);

    this.state = {
      clusterStatus: null,
      isPollingCluster: true,
      resetClusterPending: false,
      resetClusterModal: false,
      resetClusterFailure: true,
    };
  }

  componentDidMount() {
    this.createClusterInitializer(false);
  }

  async createClusterInitializer(allowClusterActions: boolean) {
    const maxActionCount = allowClusterActions ? 1 : 0;

    // Kick off an initializer which will poll for cluster status.
    try {
      this.setState({isPollingCluster: true, clusterStatus: null});
      await ClusterInitializer.initialize({
        workspaceNamespace: this.props.workspaceNamespace,
        onStatusUpdate: (clusterStatus: ClusterStatus) => {
          if (this.aborter.signal.aborted) {
            // IF we've been unmounted, don't try to update state.
            return;
          }
          this.setState({
            clusterStatus: clusterStatus,
          });
        },
        abortSignal: this.aborter.signal,
        // For the reset button, we never want to affect the cluster state. With the maxFooCount set
        // to zero, the initializer will reject the promise when it reaches a non-transitional state.
        maxDeleteCount: maxActionCount,
        maxCreateCount: maxActionCount,
        maxResumeCount: maxActionCount,
      });
      this.setState({isPollingCluster: false});
    } catch (e) {
      if (e instanceof ClusterInitializationAbortedError) {
        // Silently return if the init was aborted -- we've likely been unmounted and cannot call
        // setState anymore.
        return;
      } else if (e instanceof ClusterInitializationFailedError) {
        this.setState({clusterStatus: e.cluster ? e.cluster.status : null, isPollingCluster: false});
      } else {
        // We only expect one of the above errors, so report any other types of errors to
        // Stackdriver.
        reportError(e);
        this.setState({
          isPollingCluster: false
        });
      }
    }
  }

  componentWillUnmount() {
    this.aborter.abort();
  }

  private createTooltip(content: React.ReactFragment, children: React.ReactFragment): React.ReactFragment {
    return <TooltipTrigger content={content} side='right'>
      {children}
    </TooltipTrigger>;
  }

  private createButton(label: string, enabled: boolean, callback: () => void): React.ReactFragment {
    return <Button disabled={!enabled}
                 onClick={callback}
                 data-test-id='reset-notebook-button'
                 type='secondary'>
    {label}
    </Button>;
  }

  createButtonAndLabel(): (React.ReactFragment) {
    if (this.state.isPollingCluster) {
      const tooltipContent = <div>
        Your notebook server is still being provisioned. <br/>
        {this.state.clusterStatus != null &&
          <span>(detailed status: {this.state.clusterStatus})</span>
        }
      </div>;
      return this.createTooltip(
        tooltipContent,
        this.createButton(RESTART_LABEL, false, null));
    } else if (this.state.clusterStatus === null) {
      // If the initializer has completed and the status is null, it means that
      // a cluster doesn't exist for this workspace.
      return this.createTooltip(
        'You do not currently have an active notebook server for this workspace.',
        this.createButton(CREATE_LABEL, true, () => this.createOrResetCluster()));
    } else {
      // We usually reach this state if the cluster is at a "terminal" status and the initializer has
      // completed. This may be ClusterStatus.Stopped, ClusterStatus.Running, ClusterStatus.Error,
      // etc.
      const tooltipContent = <div>
        Your notebook server is in the following state: {this.state.clusterStatus}.
      </div>;
      return this.createTooltip(
        tooltipContent,
        this.createButton(RESTART_LABEL, true, () => this.openResetClusterModal()));
    }
  }

  render() {
    return <React.Fragment>
      <div style={styles.notebookSettings}>
        {this.createButtonAndLabel()}
      </div>
      {this.state.resetClusterModal &&
      <Modal data-test-id='reset-notebook-modal'
             loading={this.state.resetClusterPending}>
        <ModalTitle>Reset Notebook Server?</ModalTitle>
        <ModalBody>
            <div>
              <strong>Warning:</strong> Any unsaved changes to your notebooks may be lost
              and your cluster will be offline for 5-10 minutes.
              <br/><br/>
              Resetting should not be necessary under normal conditions. Please help us to
              improve this experience by using "Contact Support" from the left side's hamburger
              menu and describe the reason for this reset.
            </div>
        </ModalBody>
        <ModalFooter>
          {this.state.resetClusterFailure ?
            <div className='error'>Could not reset your notebook server.</div> : undefined}
          <Button type='secondary'
                  onClick={() => this.setState({resetClusterModal: false})}
                  data-test-id='cancel-button'>Cancel</Button>
          <Button disabled={this.state.resetClusterPending}
                  onClick={() => this.createOrResetCluster()}
                  style={{marginLeft: '0.5rem'}}
                  data-test-id='reset-cluster-send'>Reset</Button>
        </ModalFooter>
      </Modal>}
    </React.Fragment>;
  }

  openResetClusterModal(): void {
    this.setState({
      resetClusterPending: false,
      resetClusterModal: true,
      resetClusterFailure: false
    });
  }

  async createOrResetCluster(): Promise<void> {
    try {
      this.setState({resetClusterPending: true});
      if (this.state.clusterStatus === null) {
        await clusterApi().createCluster(this.props.workspaceNamespace);
      } else {
        await clusterApi().deleteCluster(this.props.workspaceNamespace);
      }
      this.setState({resetClusterPending: false, resetClusterModal: false});

      this.createClusterInitializer(true);

    } catch {
      this.setState({resetClusterPending: false, resetClusterFailure: true});
    }
  }
}
