import {Component, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {fromJS, List} from 'immutable';
import {Subscription} from 'rxjs/Subscription';

import {ReviewStateService} from '../review-state.service';

import {CohortBuilderService, DemoChartInfoListResponse, SearchRequest} from 'generated';


@Component({
    selector: 'app-overview-charts',
    templateUrl: './overview-page.html',
    styleUrls: ['./overview-page.css'],

})
export class OverviewPage implements OnInit, OnDestroy {
  openChartContainer = false;
    demoGraph = false;
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
      .switchMap(request => this.chartAPI.getDemoChartInfo(cdrVersionId, request))
      .map(response => (<DemoChartInfoListResponse>response).items)
      .subscribe(data => this.data = fromJS(data));
  }
    getCharts() {
        this.openChartContainer = true;
    }
    collapseContainer(){
        this.openChartContainer = false;
    }
    getDemoCharts (){
        this.demoGraph = true;
    }

    getConditionCharts(){
        this.demoGraph = false;
    }
  ngOnDestroy() {
    this.subscription.unsubscribe();
  }
}
