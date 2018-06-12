import {Component, OnDestroy, OnInit} from '@angular/core';

import {
  Cluster,
  ClusterService,
  ClusterStatus,
} from 'generated';

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
  private pollClusterTimer: NodeJS.Timer;
  cluster: Cluster;
  clusterDeletionFailure = true;
  resetClusterModal = false;
  resetClusterPending = false;

  constructor(private clusterService: ClusterService) {}

  ngOnInit(): void {
    this.pollCluster();
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
        if (cluster.status === ClusterStatus.Running ||
            cluster.status === ClusterStatus.Error) {
          this.cluster = cluster;
          return;
        }
        repoll();
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
