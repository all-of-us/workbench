import {Component, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';

@Component({
  selector: 'app-query-report',
  templateUrl: './query-report.component.html',
  styleUrls: ['./query-report.component.css']
})
export class QueryReportComponent implements OnInit {
  cohort: any;
  review: any;
  definition: Array<any>;

  constructor(private route: ActivatedRoute) {}

  ngOnInit() {
    const {cohort, review} = this.route.snapshot.data;
    this.cohort = cohort;
    this.review = review;
    console.log(JSON.parse(cohort.criteria));
    this.mapDefinition();
  }

  mapDefinition() {
    this.definition = [];
    const definition = JSON.parse(this.cohort.criteria);
    ['includes', 'excludes'].forEach(role => {
      if (definition[role].length) {
        const roleObj = {role, groups: []};
        definition[role].forEach(group => {
          roleObj.groups.push(this.mapGroup(group));
        });
        this.definition.push(roleObj);
      }
    });
  }

  mapGroup(group: any) {
    group.items.forEach(item => {

    });
  }

}
