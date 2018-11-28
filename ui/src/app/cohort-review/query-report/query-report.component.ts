import {Component, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';

import {TreeSubType, TreeType} from 'generated';
import {subtypeToTitle, typeToTitle} from '../../cohort-search/utils';

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
    const definition = JSON.parse(this.cohort.criteria);
    this.definition = ['includes', 'excludes'].map(role => {
      if (definition[role].length) {
        const roleObj = {role, groups: []};
        definition[role].forEach(group => {
          roleObj.groups.push(this.mapGroup(group));
        });
        return roleObj;
      }
    });
  }

  mapGroup(group: any) {
    group.items.forEach(item => {
      switch (item.type) {
        case TreeType.PM:
          this.mapPMParams(item.searchParameters);
          break;
      }
    });
  }

  mapPMParams(params: Array<any>) {
    params.forEach(param => {
      // process params
    });
  }

//  TODO create search param mapping functions for each for each domain/type with different format
}
