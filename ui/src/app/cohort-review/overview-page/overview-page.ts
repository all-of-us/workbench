import {Component, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {fromJS, List} from 'immutable';
import {Subscription} from 'rxjs/Subscription';

import {ReviewStateService} from '../review-state.service';

import {ChartInfoListResponse, CohortBuilderService, SearchRequest} from 'generated';


@Component({
  templateUrl: './overview-page.html',
})
export class OverviewPage implements OnInit, OnDestroy {

  data = List();
  private subscription: Subscription;

  constructor(
    private chartAPI: CohortBuilderService,
    private state: ReviewStateService,
    private route: ActivatedRoute,
  ) {}

  ngOnInit() {
    const {cdrVersionId} = this.route.parent.snapshot.data.workspace;
    this.subscription = this.state.cohort$
      .map(({criteria}) => <SearchRequest>(JSON.parse(criteria)))
      .switchMap(request => this.chartAPI.getChartInfo(cdrVersionId, request))
      .map(response => (<ChartInfoListResponse>response).items)
      .subscribe(data => this.data = fromJS(data));
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }
}
