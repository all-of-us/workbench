import {Component, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {CohortBuilderService} from 'generated';


@Component({
  selector: 'app-query-report',
  templateUrl: './query-report.component.html',
  styleUrls: ['./query-report.component.css']
})
export class QueryReportComponent implements OnInit {
  cohort: any;
  review: any;
  cdrId:any;
  constructor(private api: CohortBuilderService, private route: ActivatedRoute) {}

  ngOnInit() {
    const {cohort, review} = this.route.snapshot.data;
    this.cdrId = +(this.route.parent.snapshot.data.workspace.cdrVersionId);
    this.cohort = cohort;
    this.review = review;
  }

  onPrint() {
    window.print();
  }

//  TODO create search param mapping functions for each for each domain/type with different format
}
