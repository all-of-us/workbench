import {Component, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {Subscription} from 'rxjs/Subscription';

import {ReviewStateService} from '../review-state.service';

import {ChartInfoListResponse, CohortBuilderService, SearchRequest} from 'generated';


@Component({
  selector: 'app-overview',
  templateUrl: './overview.component.html',
})
export class OverviewComponent implements OnInit, OnDestroy {

  data;
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
      .subscribe(data => this.data = data);
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }
}
