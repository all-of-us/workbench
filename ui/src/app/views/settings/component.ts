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
              './component.css'],
  templateUrl: './component.html',
})
export class SettingsComponent implements OnInit, OnDestroy {
  private pollClusterTimer: NodeJS.Timer;
  cluster: Cluster;
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
        if (cluster.status === ClusterStatus.RUNNING ||
            cluster.status === ClusterStatus.ERROR) {
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
  }

  closeResetClusterModal(): void {
    this.resetClusterModal = false;
  }

  resetCluster(): void {
    this.clusterService.deleteCluster(
      this.cluster.clusterNamespace, this.cluster.clusterName).subscribe(
        () => {
          this.cluster = null;
          this.closeResetClusterModal();
          this.pollCluster();
        },
        /* onError */ undefined,
        /* onCompleted */ () => {
          this.resetClusterPending = false;
        });
  }
}
