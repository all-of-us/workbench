import {Component, EventEmitter, OnDestroy, OnInit, Output} from '@angular/core';
import {cohortReviewStore} from 'app/cohort-review/review-state.service';
import {cohortBuilderApi, cohortReviewApi} from 'app/services/swagger-fetch-clients';
import {currentCohortStore, currentWorkspaceStore, urlParamsStore} from 'app/utils/navigation';
import {DemoChartInfoListResponse, DomainType, SearchRequest} from 'generated/fetch';
import {CohortReview} from 'generated/fetch';
import {fromJS, List} from 'immutable';
import {from} from 'rxjs/observable/from';
import {Subscription} from 'rxjs/Subscription';


@Component({
  selector: 'app-overview-charts',
  templateUrl: './overview-page.html',
  styleUrls: ['./overview-page.css'],

})
export class OverviewPage implements OnInit, OnDestroy {
  openChartContainer = false;
  demoGraph = false;
  @Output() dataItems = new EventEmitter<any>();
  data = List();
  typesList = [DomainType[DomainType.CONDITION],
    DomainType[DomainType.PROCEDURE],
    DomainType[DomainType.DRUG],
    DomainType[DomainType.LAB]];
  title: string;
  showTitle = false;
  review: CohortReview;
  totalParticipantCount: number;
  buttonsDisableFlag = false;
  private subscription: Subscription;
  domainsData = {};
  totalCount: any;
  constructor() {}

  ngOnInit() {
    const workspace = currentWorkspaceStore.getValue();
    const cohort = currentCohortStore.getValue();
    const request = <SearchRequest>(JSON.parse(cohort.criteria));
    from(cohortBuilderApi().getDemoChartInfo(+workspace.cdrVersionId, request))
      .map(response => (<DemoChartInfoListResponse>response).items)
      .subscribe(data => {
        this.data = fromJS(data);
        this.dataItems.emit(this.data);
        this.buttonsDisableFlag = false;
      });
    this.review = cohortReviewStore.getValue();
    this.totalParticipantCount = this.review.matchedParticipantCount;


    this.openChartContainer = true;
    this.fetchChartsData();
  }


  fetchChartsData() {
    this.demoGraph = false;

    this.buttonsDisableFlag = true;
    this.showTitle = false;
    const limit = 10;
    const cdrid = +(currentWorkspaceStore.getValue().cdrVersionId);
    const {ns, wsid, cid} = urlParamsStore.getValue();
    this.typesList.map(domainName => {
      this.domainsData[domainName] = {
        conditionTitle: '',
        loading: true
      };
      this.subscription = from(cohortReviewApi()
        .getCohortChartData(ns, wsid, cid, cdrid, domainName, limit, null))
        .subscribe(data => {
          const chartData = data;
          this.totalCount = chartData.count;
          this.domainsData[domainName] = chartData.items;
          this.domainsData[domainName].loading = false;
        });
    });
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

}
