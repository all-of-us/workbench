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

  // async ppiCheck(definition: any) {
  //   const parents = {};
  //   for (const role in definition) {
  //     if (definition.hasOwnProperty(role)) {
  //       for (const group of definition[role]) {
  //         for (const item of group.items) {
  //           if (item.type !== TreeType[TreeType.PPI]) {
  //             continue;
  //           }
  //           for (const param of item.searchParameters) {
  //             const name = await this.getPPIParent(param.conceptId);
  //             parents[param.conceptId] = name;
  //           }
  //         }
  //       }
  //     }
  //   }
  //   return new Promise(resolve => {
  //     resolve(parents);
  //   });
  // }
  //
  // async getPPIParent(conceptId: string) {
  //   let name;
  //   await this.api
  //     .getPPICriteriaParent(this.review.cdrVersionId, TreeType[TreeType.PPI], conceptId)
  //     .toPromise()
  //     .then(parent => name = parent.name);
  //   return name;
  // }

  onPrint() {
    window.print();
  }

//  TODO create search param mapping functions for each for each domain/type with different format
}
