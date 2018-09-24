import {NgRedux, select} from '@angular-redux/store';
import {Component, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {
    activeModifierList,
    activeParameterList,
    CohortSearchActions,
    CohortSearchState,
    isChartLoading,
    isCriteriaLoading, isParameterActive
} from '../../cohort-search/redux';
import {CohortBuilderService, CohortReviewService, DemoChartInfoListResponse, DomainType, SearchRequest} from 'generated';
import {fromJS, List} from 'immutable';
import {Subscription} from 'rxjs/Subscription';
import {ReviewStateService} from '../review-state.service';
import {Observable} from "rxjs/Observable";
import {Participant} from "../participant.model";
import {CohortReview} from "../../../generated";



@Component({
    selector: 'app-overview-charts',
    templateUrl: './overview-page.html',
    styleUrls: ['./overview-page.css'],

})
export class OverviewPage implements OnInit, OnDestroy {
  openChartContainer = false;
  demoGraph = false;
  data = List();
  typesList= [DomainType[DomainType.CONDITION],
              DomainType[DomainType.PROCEDURE],
              DomainType[DomainType.MEASUREMENT],
              DomainType[DomainType.LAB]];
  title: string;
  showTitle = false;
  private subscription: Subscription;
  loading: any;
  domainItems = [];
  spinner = false;
  selectedCohortName: string;
  review: CohortReview;
  totalParticipantCount: number;

  constructor(
    private ngRedux: NgRedux<CohortSearchState>,
    private chartAPI: CohortBuilderService,
    private reviewAPI: CohortReviewService,
    private state: ReviewStateService,
    private route: ActivatedRoute,
    private actions: CohortSearchActions,
  ) {}

  ngOnInit() {
     this.selectedCohortName = this.route.parent.snapshot.data.cohort.name
      this.spinner = true;
      const {cdrVersionId} = this.route.parent.snapshot.data.workspace;
      this.subscription = this.state.cohort$
          .map(({criteria}) => <SearchRequest>(JSON.parse(criteria)))
          .switchMap(request => this.chartAPI.getDemoChartInfo(cdrVersionId, request))
          .map(response => (<DemoChartInfoListResponse>response).items)
          .subscribe(data => {
              this.data = fromJS(data)
              this.spinner = false;
          });

      this.subscription = this.state.review$.subscribe(review => {
          this.review = review;
          this.totalParticipantCount = review.matchedParticipantCount;

      });
      this.getDemoCharts();
      this.getCharts();
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
        this.title = '';
    }

    getDifferentCharts(names){
        this.demoGraph = false;
        // this.showTitle = true;
        this.title = names;
        this.fetchChartsData(names);
        return this.title;

    }

    fetchChartsData(name){
      this.spinner = true;
      this.showTitle = false;
      const domain = name
      const limit = 10;
      const {ns, wsid, cid} = this.route.parent.snapshot.params;
      const cdrid = +(this.route.parent.snapshot.data.workspace.cdrVersionId);
      this.actions.fetchReviewChartsData(ns, wsid, cid, cdrid, domain, limit);
      const loadingSub = this.ngRedux
        .select(isChartLoading(domain))
        .filter(domain => !!domain)
        .subscribe(loading => {
            this.spinner = false;
            this.showTitle = true;
            this.loading = loading;
            const totalCount = this.loading.toJS().count;
            this.domainItems = this.loading.toJS().items;
            this.domainItems.forEach(itemCount=> {
                let test = itemCount.count;
                let percentCount = ((test / totalCount) * 100);
               Object.assign(itemCount, {percentCount: percentCount});
            });
            // console.log(this.domainItems);
        });
       this.subscription = loadingSub;
    }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }
}
