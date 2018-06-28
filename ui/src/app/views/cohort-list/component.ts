import {Component, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';

import {WorkspaceData} from 'app/resolvers/workspace';

import {
  Cohort,
  CohortsService,
  Workspace,
} from 'generated';


@Component({
  styleUrls: ['./component.css',
    '../../styles/buttons.css',
    '../../styles/cards.css'],
  templateUrl: './component.html',
})
export class CohortListComponent implements OnInit, OnDestroy {
  cohortList: Cohort[] = [];
  workspace: Workspace;
  cohortsLoading = true;
  cohortsError = false;
  wsNamespace: string;
  wsId: string;

  constructor(
    private route: ActivatedRoute,
    private cohortsService: CohortsService,
  ) {
    const wsData: WorkspaceData = this.route.snapshot.data.workspace;
    this.workspace = wsData;
  }

  ngOnInit(): void {
    this.wsNamespace = this.route.snapshot.params['ns'];
    this.wsId = this.route.snapshot.params['wsid'];
    this.cohortsService.getCohortsInWorkspace(this.wsNamespace, this.wsId)
      .subscribe(
        cohortsReceived => {
          for (const coho of cohortsReceived.items) {
            this.cohortList.push(coho);
          }
          this.cohortsLoading = false;
        },
        error => {
          this.cohortsLoading = false;
          this.cohortsError = true;
        });
  }

  ngOnDestroy(): void {

  }

  createNotebook(): void {
  }
}
