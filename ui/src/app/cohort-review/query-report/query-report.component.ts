import {AfterContentChecked, ChangeDetectorRef, Component, OnInit} from '@angular/core';
import {cohortReviewStore} from 'app/cohort-review/review-state.service';
import {WorkspaceData} from 'app/resolvers/workspace';
import {CdrVersionStorageService} from 'app/services/cdr-version-storage.service';
import {WorkspaceData} from 'app/services/workspace-storage.service';
import {currentCohortStore, currentWorkspaceStore} from 'app/utils/navigation';
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
  workspace: WorkspaceData;

  constructor(
    private cdref: ChangeDetectorRef,
    private cdrVersionStorageService: CdrVersionStorageService) {}

  ngOnInit() {
    this.cohort = currentCohortStore.getValue();
    this.workspace = currentWorkspaceStore.getValue();
    this.review = cohortReviewStore.getValue();
    this.cdrVersionStorageService.cdrVersions$.subscribe(resp => {
      this.cdrDetails = resp.items.find(v => v.cdrVersionId === this.workspace.cdrVersionId);
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
