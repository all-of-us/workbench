import {Component, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';

import {CohortBuilderService, TreeSubType, TreeType} from 'generated';
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
  constructor(private api: CohortBuilderService, private route: ActivatedRoute) {}

  ngOnInit() {
    const {cohort, review} = this.route.snapshot.data;
    this.cohort = cohort;
    this.review = review;
    // console.log(JSON.parse(cohort.criteria));
    this.mapDefinition();
  }

  mapDefinition() {
    const definition = JSON.parse(this.cohort.criteria)
    this.definition = ['includes', 'excludes'].map(role => {
      if (definition[role].length) {
        const roleObj = {role, groups: []};
        definition[role].forEach(group => {
          roleObj.groups.push(this.mapGroup(group));
        });
        return roleObj;
      }
    });
    // console.log( this.definition)
  }

  mapGroup(group: any) {
    return group.items.map(item => {
      switch (item.type) {
        case TreeType.PM:
          return this.mapPMParams(item.searchParameters);
        case TreeType.PPI:
          return this.mapPPIParams(item.searchParameters);
        default:
          return this.mapParams(item.type, item.searchParameters);
      }
    });
  }

  mapPMParams(params: Array<any>) {
    return params.map(param => {
       return {
         items: typeToTitle(param.type) + ' | ' + param.name,
         type: param.type
       };
    });
  }

  async mapPPIParams(params: Array<any>) {
    const questions = {};
    console.log(params);
    for (const param of params) {
      questions[param.conceptId] = await this.getPPIParent(param.conceptId.toString());
      console.log(questions);
    }
    return params.map(param => {
      return {
        items: typeToTitle(param.type) + ' | ' + questions[param.conceptId] + ' | ' + param.name,
        type: param.type
      };
    });
  }

  mapParams(_type: string, params: Array<any>) {
    return params.map(param => {
       return {
         items: typeToTitle(_type) + ' | ' + param.subtype + ' | ' + param.name,
         type: param.type
       };
    });
  }

  async getPPIParent(conceptId: string) {
    this.api.getPPICriteriaParent(this.review.cdrVersionId, TreeType[TreeType.PPI], conceptId)
      .subscribe(crit => {
        console.log(crit);
        return crit.name;
      });
  }

  onPrint() {
    window.print();
  }
//  TODO create search param mapping functions for each for each domain/type with different format
}
