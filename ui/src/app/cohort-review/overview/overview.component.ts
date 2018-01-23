import {Component, OnDestroy, OnInit} from '@angular/core';
import {Subscription} from 'rxjs/Subscription';

import {ReviewStateService} from '../review-state.service';

import {CohortBuilderService, SearchRequest} from 'generated';


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
  ) {}

  ngOnInit() {
    this.subscription = this.state.cohort$
      .map(({criteria}) => <SearchRequest>(JSON.parse(criteria)))
      .switchMap(request => this.chartAPI.getChartInfo(request))
      .map(response => response.items)
      .subscribe(data => this.data = data);
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }
}
