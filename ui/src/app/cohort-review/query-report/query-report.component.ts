import {AfterContentChecked, ChangeDetectorRef, Component, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {CohortBuilderService, Workspace} from 'generated';
import {List} from 'immutable';
import {Observable} from 'rxjs/Observable';
import {CdrVersionStorageService} from "../../services/cdr-version-storage.service";
import {WorkspaceData} from '../../resolvers/workspace';



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
  workspace: Workspace;

  constructor(private api: CohortBuilderService,
              private route: ActivatedRoute,
              private cdref: ChangeDetectorRef,
              private cdrVersionStorageService: CdrVersionStorageService) {}

  ngOnInit() {
    const {cohort, review} = this.route.snapshot.data;
    const wsData: WorkspaceData = this.route.snapshot.data.workspace;
    this.workspace = wsData;
    this.cohort = cohort;
    this.review = review;
    this.cdrVersionStorageService.cdrVersions$.subscribe(resp =>
    { this.cdrDetails = resp.items.find(v => v.cdrVersionId === this.workspace.cdrVersionId); });
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
