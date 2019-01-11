import {Component, OnDestroy, OnInit} from '@angular/core';

import * as React from 'react';
import * as ReactDOM from 'react-dom';

import {Button} from 'app/components/buttons';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {TooltipTrigger} from 'app/components/popups';

import {
  Cluster,
  ClusterService,
  ClusterStatus,
} from 'generated';

const styles = {
  notebookSettings: {
    marginTop: '1rem'
  },
};

export class SettingsReact extends React.Component<{}, {}> {
  private static readonly TRANSITIONAL_STATUSES = new Set<ClusterStatus>([
    ClusterStatus.Deleting, ClusterStatus.Creating,
    ClusterStatus.Starting, ClusterStatus.Stopping
  ]);

  cluster: Cluster;
  resetClusterPending = false;
  resetClusterModal = false;
  clusterDeletionFailure = true;

  private pollClusterTimer: NodeJS.Timer;

  render() {
    return <React.Fragment>
      <h2>Settings</h2>
      <h3>Notebooks</h3>
      <div style={styles.notebookSettings}>
        <TooltipTrigger content={
                          (!this.cluster) ?
                            'Your notebook server is still being created' : undefined}
                        side='right'>
          <Button disabled={!this.cluster} onClick={this.openResetClusterModal()}
                  type='secondary'>
            Reset Notebook Server
          </Button>
        </TooltipTrigger>
      </div>
      <Modal onRequestClose={this.closeResetClusterModal()}>
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
          {this.clusterDeletionFailure ?
            <div className='error'>Could not reset your notebook server.</div> : undefined}
          <Button type='secondary'
                  onClick={() => this.closeResetClusterModal()}>Cancel</Button>
          <Button disabled={this.resetClusterPending}
                  onClick={() => this.resetCluster()}>Send</Button>
        </ModalFooter>
      </Modal>
    </React.Fragment>;
  }

  openResetClusterModal(): void {
    this.resetClusterPending = false;
    this.resetClusterModal = true;
    this.clusterDeletionFailure = false;
  }

  closeResetClusterModal(): void {
    this.resetClusterModal = false;
  }

  resetCluster(): void {
    this.clusterService.deleteCluster(
      this.cluster.clusterNamespace, this.cluster.clusterName).subscribe(
      () => {
        this.cluster = null;
        this.resetClusterPending = false;
        this.closeResetClusterModal();
        this.pollCluster();
      },
      /* onError */ () => {
        this.resetClusterPending = false;
        this.clusterDeletionFailure = true;
      },
      /* onCompleted */ () => {
        this.resetClusterPending = false;
      });
  }

  private pollCluster(): void {
    const repoll = () => {
      this.pollClusterTimer = setTimeout(() => this.pollCluster(), 15000);
    };
    this.clusterService.listClusters()
      .subscribe((resp) => {
        const cluster = resp.defaultCluster;
        if (SettingsReact.TRANSITIONAL_STATUSES.has(cluster.status)) {
          repoll();
          return;
        }
        this.cluster = cluster;
      }, () => {
        // Also retry on errors.
        repoll();
      });
  }
}

@Component({
  styleUrls: ['../../styles/buttons.css',
              '../../styles/cards.css',
              '../../styles/headers.css',
              '../../styles/inputs.css',
              '../../styles/errors.css',
              './component.css'],
  templateUrl: './component.html',
})
export class SettingsComponent implements OnInit, OnDestroy {
  private static readonly TRANSITIONAL_STATUSES = new Set<ClusterStatus>([
    ClusterStatus.Deleting, ClusterStatus.Creating,
    ClusterStatus.Starting, ClusterStatus.Stopping
  ]);

  private pollClusterTimer: NodeJS.Timer;
  cluster: Cluster;
  clusterDeletionFailure = true;
  resetClusterModal = false;
  resetClusterPending = false;

  constructor(private clusterService: ClusterService) {}

  ngOnInit(): void {
    ReactDOM.render(<SettingsReact/>, document.getElementById('settings-page'));
    // this.pollCluster();
  }

  ngOnDestroy(): void {
    if (this.pollClusterTimer) {
      clearTimeout(this.pollClusterTimer);
    }
  }

  private pollCluster(): void {
    const repoll = () => {
      this.pollClusterTimer = setTimeout(() => this.pollCluster(), 15000);
    };
    this.clusterService.listClusters()
      .subscribe((resp) => {
        const cluster = resp.defaultCluster;
        if (SettingsComponent.TRANSITIONAL_STATUSES.has(cluster.status)) {
          repoll();
          return;
        }
        this.cluster = cluster;
      }, () => {
        // Also retry on errors.
        repoll();
      });
  }

  openResetClusterModal(): void {
    this.resetClusterPending = false;
    this.resetClusterModal = true;
    this.clusterDeletionFailure = false;
  }

  closeResetClusterModal(): void {
    this.resetClusterModal = false;
  }

  resetCluster(): void {
    this.clusterService.deleteCluster(
      this.cluster.clusterNamespace, this.cluster.clusterName).subscribe(
        () => {
          this.cluster = null;
          this.resetClusterPending = false;
          this.closeResetClusterModal();
          this.pollCluster();
        },
        /* onError */ () => {
        this.resetClusterPending = false;
        this.clusterDeletionFailure = true;
        },
        /* onCompleted */ () => {
          this.resetClusterPending = false;
        });
  }
}
