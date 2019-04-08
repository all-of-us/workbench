import {AfterContentChecked, ChangeDetectorRef, Component, OnInit} from '@angular/core';
import {cohortReviewStore} from 'app/cohort-review/review-state.service';
import {CdrVersionStorageService} from 'app/services/cdr-version-storage.service';
import {currentCohortStore} from 'app/utils/navigation';
import {List} from 'immutable';
import {Observable} from 'rxjs/Observable';




@Component({
  selector: 'app-query-report',
  templateUrl: './query-report.component.html',
  styleUrls: ['./query-report.component.css']
})
export class QueryReportComponent implements OnInit, AfterContentChecked {
  cohort: any;
  review: any;
  cdrDetails: any ;
  data:  Observable<List<any>>;

  constructor(
    private cdref: ChangeDetectorRef,
    private cdrVersionStorageService: CdrVersionStorageService) {}

  ngOnInit() {
    this.cohort = currentCohortStore.getValue();
    this.review = cohortReviewStore.getValue();
    this.cdrVersionStorageService.cdrVersions$.subscribe(resp => {
      this.cdrDetails = resp.items.find(
        v => v.cdrVersionId === this.review.cdrVersionId.toString()
      );
    });
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
}
