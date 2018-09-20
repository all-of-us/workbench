import {Component, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {fromJS, List} from 'immutable';
import {Subscription} from 'rxjs/Subscription';

import {ReviewStateService} from '../review-state.service';

import {CohortBuilderService, DemoChartInfoListResponse, SearchRequest} from 'generated';
import {TreeType} from "../../../generated";
import {typeToTitle} from "../../cohort-search/utils";


@Component({
    selector: 'app-overview-charts',
    templateUrl: './overview-page.html',
    styleUrls: ['./overview-page.css'],

})
export class OverviewPage implements OnInit, OnDestroy {
  openChartContainer = false;
  demoGraph = false;
  data = List();
  typesList= ['CONDITIONS','PROCEDURES', 'MEDICATION', 'LABS'];
  title: string;
  showTitle = false;
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

    this.getDemoCharts();
    this.getCharts(); // if only one flag in future modify the method;
  }
    getCharts() {
        this.openChartContainer = true;
    }
    collapseContainer(){
        this.openChartContainer = false;
    }
    getDemoCharts (){
        this.demoGraph = true;
        this.showTitle = false;
    }

    getDifferentCharts(names){
      console.log(names);
        this.demoGraph = false;
        this.showTitle = true;
        this.title = names;
        return this.title;
    }

    //
    // get selectionTitle() {
    //     const _type = [
    //         TreeType[TreeType.CONDITION],
    //         TreeType[TreeType.PROCEDURE]
    //     ].includes(this.itemType)
    //         ? this.itemType : this.ctype;
    //     const title = typeToTitle(_type);
    //     return title
    // }
  ngOnDestroy() {
    this.subscription.unsubscribe();
  }
}
