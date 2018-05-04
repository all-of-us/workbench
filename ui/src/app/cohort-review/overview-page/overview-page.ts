import {Component, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {fromJS, List} from 'immutable';
import {Subscription} from 'rxjs/Subscription';

import {ReviewStateService} from '../review-state.service';

import {WorkspaceStorageService} from 'app/services/workspace-storage.service';

import {ChartInfoListResponse, CohortBuilderService, SearchRequest} from 'generated';


@Component({
  templateUrl: './overview-page.html',
})
export class OverviewPage implements OnInit, OnDestroy {

  data = List();
  private cdrId: number;
  private subscription: Subscription;

  constructor(
    private chartAPI: CohortBuilderService,
    private state: ReviewStateService,
    private route: ActivatedRoute,
    private workspaceStorageService: WorkspaceStorageService,
  ) {}

  ngOnInit() {
    this.workspaceStorageService.activeWorkspace$.subscribe((workspace) => {
      this.cdrId = parseInt(workspace.cdrVersionId, 10);
    });
    console.log(this.route);
    this.workspaceStorageService.reloadIfNew(
      this.route.snapshot.parent.params['ns'],
      this.route.snapshot.parent.params['wsid']);

    this.subscription = this.state.cohort$
      .map(({criteria}) => <SearchRequest>(JSON.parse(criteria)))
      .switchMap(request => this.chartAPI.getChartInfo(this.cdrId, request))
      .map(response => (<ChartInfoListResponse>response).items)
      .subscribe(data => this.data = fromJS(data));
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }
}
