import {Component, OnInit} from '@angular/core';

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
export class SettingsComponent implements OnInit {
  cluster: Cluster;
  resetClusterModal = false;
  resetClusterPending = false;

  constructor(private clusterService: ClusterService) {}

  ngOnInit(): void {
    this.pollCluster();
  }

  private pollCluster(): void {
    this.clusterService.listClusters()
      .subscribe((resp) => {
        const cluster = resp.defaultCluster;
        if (cluster.status !== ClusterStatus.CREATING) {
          this.cluster = cluster;
          return;
        }
        setTimeout(() => this.pollCluster(), 10000);
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
