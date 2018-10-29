import {NgRedux} from '@angular-redux/store';
import {Component, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {CohortBuilderService, CohortReview, CohortReviewService, DemoChartInfoListResponse, DomainType, SearchRequest} from 'generated';
import {fromJS, List} from 'immutable';
import {Subscription} from 'rxjs/Subscription';
import {typeToTitle} from '../../cohort-search/utils';
import {ReviewStateService} from '../review-state.service';


@Component({
  selector: 'app-overview-charts',
  templateUrl: './overview-page.html',
  styleUrls: ['./overview-page.css'],

})
export class OverviewPage implements OnInit, OnDestroy {
  openChartContainer = false;
  demoGraph = false;
  data = List();
  typesList = [DomainType[DomainType.CONDITION],
    DomainType[DomainType.PROCEDURE],
    DomainType[DomainType.DRUG],
    DomainType[DomainType.LAB]];
  title: string;
  showTitle = false;
  loading: any;
  selectedCohortName: string;
  review: CohortReview;
  totalParticipantCount: number;
  isCancelTimerInitiated: any = false;
  domainTitle: '';
  buttonsDisableFlag = false;
  private subscription: Subscription;
  conditionDomainItems = [];
  procedureDomainItems = [];
  drugDomainItems = [];
  labDomainItems = [];
  condChart: any;
  totalCount: any;
  constructor(
    private chartAPI: CohortBuilderService,
    private reviewAPI: CohortReviewService,
    private state: ReviewStateService,
    private route: ActivatedRoute,
  ) {}

  ngOnInit() {
    this.selectedCohortName = this.route.parent.snapshot.data.cohort.name;
    const {cdrVersionId} = this.route.parent.snapshot.data.workspace;
    this.subscription = this.state.cohort$
      .map(({criteria}) => <SearchRequest>(JSON.parse(criteria)))
      .switchMap(request => this.chartAPI.getDemoChartInfo(cdrVersionId, request))
      .map(response => (<DemoChartInfoListResponse>response).items)
      .subscribe(data => {
        this.data = fromJS(data);
        this.buttonsDisableFlag = false;
      });
    this.subscription = this.state.review$.subscribe(review => {
      this.review = review;
      this.totalParticipantCount = review.matchedParticipantCount;

    });
    this.getDemoCharts();
    this.openChartContainer = true;
    this.fetchChartsData();
  }


  fetchChartsData() {
    this.demoGraph = false;

    this.buttonsDisableFlag = true;
    this.showTitle = false;
    const limit = 10;
    const cdrid = +(this.route.parent.snapshot.data.workspace.cdrVersionId);
    const {ns, wsid, cid} = this.route.parent.snapshot.params;
    this.typesList.map(domainName => {
      this.subscription = this.reviewAPI.getCohortChartData(ns, wsid, cid, cdrid, domainName,
        limit, null)
        .subscribe(loading => {
          this.loading = loading;
          this.totalCount = this.loading.count;
          if (domainName === DomainType[DomainType.CONDITION]) {
            this.conditionDomainItems = this.loading.items;
          } else if (domainName === DomainType[DomainType.PROCEDURE]) {
            this.procedureDomainItems = this.loading.items;
          } else if (domainName === DomainType[DomainType.DRUG]) {
            this.drugDomainItems = this.loading.items;
          } else {
            this.labDomainItems = this.loading.items;
          }
        });
    });
  }


  getDemoCharts () {
    this.buttonsDisableFlag = true;
    this.condChart = '';
    setTimeout(() => {
      this.demoGraph = true;
      if (this.data.size) {
        this.buttonsDisableFlag = false;
      }
    }, 1000);
    this.showTitle = false;
    this.domainTitle = '';
    this.isCancelTimerInitiated = false;
  }

  getDifferentCharts(names) {
    this.buttonsDisableFlag = true;
    this.demoGraph = false;
    this.domainTitle = names;
    this.showTitle = true;
    if (names === DomainType[DomainType.CONDITION]) {
      this.condChart = '';
      this.setNames(names);
    } else if (names === DomainType[DomainType.PROCEDURE]) {
       this.condChart = '';
      this.setNames(names);
    } else if (names === DomainType[DomainType.DRUG]) {
      this.condChart = '';
      this.setNames(names);
    } else {
      this.condChart = '';
      this.setNames(names);
    }
    this.title = typeToTitle(names);
    return this.title;
  }

  setNames(names) {
    setTimeout(() => {
      this.condChart = names;
      this.buttonsDisableFlag = false;
    }, 1000);
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

}







