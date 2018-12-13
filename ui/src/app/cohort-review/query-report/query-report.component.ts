import { AfterContentChecked, ChangeDetectorRef, Component, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {CohortBuilderService} from 'generated';
import {Observable} from "rxjs/Observable";
import {List} from "immutable";


@Component({
  selector: 'app-query-report',
  templateUrl: './query-report.component.html',
  styleUrls: ['./query-report.component.css']
})
export class QueryReportComponent implements OnInit, AfterContentChecked {
  cohort: any;
  review: any;
  cdrId: any;
  data:  Observable<List<any>>;
  constructor(private api: CohortBuilderService, private route: ActivatedRoute, private cdref: ChangeDetectorRef) {}

  ngOnInit() {
    const {cohort, review} = this.route.snapshot.data;
    this.cdrId = +(this.route.parent.snapshot.data.workspace.cdrVersionId);
    this.cohort = cohort;
    this.review = review;
  }
  ngAfterContentChecked() {
    this.cdref.detectChanges();
  }
  getDemoChartData(d) {
    this.ngAfterContentChecked();
    if (d) {
      this.data = d.toJS();
    }
  }

//  TODO create search param mapping functions for each for each domain/type with different format
}
