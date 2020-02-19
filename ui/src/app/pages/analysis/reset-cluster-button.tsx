import * as React from 'react';

import {Button} from 'app/components/buttons';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {TooltipTrigger} from 'app/components/popups';
import {clusterApi} from 'app/services/swagger-fetch-clients';

import {
  Cluster,
  ClusterStatus,
} from 'generated/fetch/api';



export const TRANSITIONAL_STATUSES = new Set<ClusterStatus>([
  ClusterStatus.Creating,
  ClusterStatus.Starting,
  ClusterStatus.Stopping,
  ClusterStatus.Deleting,
]);

const styles = {
  notebookSettings: {
    marginTop: '1rem'
  },
};

export interface Props {
  workspaceNamespace: string;
}

interface State {
  cluster: Cluster;
  resetClusterPending: boolean;
  resetClusterModal: boolean;
  resetClusterFailure: boolean;
  clusterTransitionStatus?: ClusterStatus;
}

export class ResetClusterButton extends React.Component<Props, State> {

  private pollClusterTimer: NodeJS.Timer;

  constructor(props) {
    super(props);

    this.state = {
      cluster: null,
      resetClusterPending: false,
      resetClusterModal: false,
      resetClusterFailure: true,
      clusterTransitionStatus: null,
    };
  }

  componentDidMount() {
    this.pollCluster();
  }

  componentWillUnmount() {
    if (this.pollClusterTimer) {
      clearTimeout(this.pollClusterTimer);
    }
  }

  render() {
    const {clusterTransitionStatus} = this.state;

    return <React.Fragment>
      <div style={styles.notebookSettings}>
        <TooltipTrigger content={
            !this.state.cluster ?
              <div>Your notebook server is still being created <br/>
                (Detailed status: {clusterTransitionStatus}) </div> : undefined}
                        side='right'>
          <Button disabled={!this.state.cluster}
                  onClick={() => this.openResetClusterModal()}
                  data-test-id='reset-notebook-button'
                  type='secondary'>
            Reset Notebook Server
          </Button>
        </TooltipTrigger>
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
                  onClick={() => this.resetCluster()}
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

  async resetCluster(): Promise<void> {
    const {clusterName, clusterNamespace} = this.state.cluster;

    try {
      this.setState({ resetClusterPending: true });
      await clusterApi().deleteCluster(clusterNamespace);
      this.setState({resetClusterPending: false});
      this.pollCluster();
    } catch {
      this.setState({resetClusterPending: false, resetClusterFailure: true});
    }
  }

  private async pollCluster(): Promise<void> {
    const {workspaceNamespace} = this.props;

    const repoll = () => {
      this.pollClusterTimer = setTimeout(() => this.pollCluster(), 15000);
    };

    try {
      let cluster = await clusterApi().getCluster(workspaceNamespace);
      if (cluster == null) {
        cluster = await clusterApi().createCluster(workspaceNamespace);
      }

      if (TRANSITIONAL_STATUSES.has(cluster.status)) {
        this.setState({clusterTransitionStatus: cluster.status});

        // Keep polling if we're still waiting for the cluster to start up.
        repoll();
        return;
      }

      // Only store the cluster in React state when it's in a ready-state.
      this.setState({cluster: cluster, clusterTransitionStatus: null});
    } catch (e) {
      // Also re-poll on any errors.
      repoll();
    }
  }
}

