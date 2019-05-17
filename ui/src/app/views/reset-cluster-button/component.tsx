import * as React from 'react';

import {Button} from 'app/components/buttons';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {TooltipTrigger} from 'app/components/popups';
import {clusterApi} from 'app/services/swagger-fetch-clients';

import {
  Cluster,
  ClusterStatus,
} from 'generated/fetch/api';
import {Component, Input} from "@angular/core";
import {ReactWrapperBase} from "../../utils";

const TRANSITIONAL_STATUSES = new Set<ClusterStatus>([
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

interface Props {
  billingProjectId: string
}

interface State {
  cluster: Cluster;
  resetClusterPending: boolean;
  resetClusterModal: boolean;
  clusterDeletionFailure: boolean;
}

class ResetClusterButton extends React.Component<Props, State> {

  private pollClusterTimer: NodeJS.Timer;

  constructor(props) {
    super(props);

    this.state = {
      cluster: null,
      resetClusterPending: false,
      resetClusterModal: false,
      clusterDeletionFailure: true,
    };
  }

  componentDidMount() {
    this.pollCluster(this.props.billingProjectId);
  }

  componentWillUnmount() {
    if (this.pollClusterTimer) {
      clearTimeout(this.pollClusterTimer);
    }
  }

  render() {
    return <React.Fragment>
      <div style={styles.notebookSettings}>
        <TooltipTrigger content={
            !this.state.cluster ? 'Your notebook server is still being created' : undefined}
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
          <strong>Warning:</strong> Any unsaved changes to your notebooks may be lost
          and your cluster will be offline for 5-10 minutes.
          <br/><br/>
          Resetting should not be necessary under normal conditions. Please help us to
          improve this experience by using "Report a Bug" from the profile drop-down
          to describe the reason for this reset.
        </ModalBody>
        <ModalFooter>
          {this.state.clusterDeletionFailure ?
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
      clusterDeletionFailure: false
    });
  }

  resetCluster(): void {
    const clusterBillingProjectId = this.state.cluster.clusterNamespace;
    clusterApi().deleteCluster(this.state.cluster.clusterNamespace, this.state.cluster.clusterName)
      .then(() => {
        this.setState({cluster: null, resetClusterPending: false, resetClusterModal: false});
        this.pollCluster(clusterBillingProjectId);
      })
      .catch(() => {
        this.setState({resetClusterPending: false, clusterDeletionFailure: true});
      });
  }

  private pollCluster(billingProjectId): void {
    const repoll = () => {
      this.pollClusterTimer = setTimeout(() => this.pollCluster(billingProjectId), 15000);
    };
    clusterApi().listClusters(billingProjectId)
      .then((body) => {
        const cluster = body.defaultCluster;
        if (TRANSITIONAL_STATUSES.has(cluster.status)) {
          repoll();
          return;
        }
        this.setState({cluster: cluster});
      })
      .catch(() => {
        // Also retry on errors
        repoll();
      });
  }
}

@Component({
  selector: 'react-cluster-button',
  template: '<div #root></div>'
})
class ResetClusterButtonComponent extends ReactWrapperBase {
  @Input() billingProjectId;

  constructor() {
    super(ResetClusterButton, ['billingProjectId']);
  }
}

export {
  Props as ResetClusterButtonProps,
  ResetClusterButton,
  ResetClusterButtonComponent
}