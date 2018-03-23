import {Component, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';

import {
  CohortReviewService,
  PageFilterRequest,
  PageFilterType,
  ParticipantConditionsColumns,
  SortOrder,
} from 'generated';


@Component({
  selector: 'app-detail-conditions',
  templateUrl: './detail-conditions.component.html',
  styleUrls: ['./detail-conditions.component.css']
})
export class DetailConditionsComponent implements OnInit {
  conditions;
  apiCaller;
  loading;
  request;
  totalCount = 1000;

  constructor(
    private route: ActivatedRoute,
    private reviewApi: CohortReviewService,
  ) {}

  ngOnInit() {
    // console.dir(this.route);
    const {participant} = this.route.snapshot.data;
    const {cohort, workspace} = this.route.snapshot.parent.data;

    this.apiCaller = (request) => this.reviewApi.getParticipantConditions(
      workspace.namespace,
      workspace.id,
      cohort.id,
      workspace.cdrVersionId,
      participant.participantId,
      request
    );

    this.request = <PageFilterRequest>{
      page: 0,
      pageSize: 25,
      includeTotal: true,
      sortOrder: SortOrder.Asc,
      pageFilterType: PageFilterType.ParticipantConditionsPageFilter,
      sortColumn: ParticipantConditionsColumns.ItemDate,
    };

    this.callApi();
  }

  callApi() {
    this.loading = true;
    this.apiCaller(this.request).subscribe(resp => {
      this.conditions = resp.items;
      // this.totalCount = resp.count;
      // this.request = resp.request;
      this.loading = false;
    });
  }

    /*
  toPage(page: number) {
    if (!this.loading) {
      this.request = {...this.request, page};
      this.callApi();
    }
  }
     */
}
