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
  ppiParents: any;
  constructor(private api: CohortBuilderService, private route: ActivatedRoute) {}

  ngOnInit() {
    const {cohort, review} = this.route.snapshot.data;
    this.cohort = cohort;
    this.review = review;
    this.mapDefinition();
  }

  mapDefinition() {
    const definition = JSON.parse(this.cohort.criteria)
    this.ppiCheck(definition).then(parents => {
      this.ppiParents = parents;
      console.log(this.ppiParents);
      this.definition = ['includes', 'excludes'].map(role => {
        if (definition[role].length) {
          const roleObj = {role, groups: []};
          definition[role].forEach(group => {
            roleObj.groups.push(this.mapGroup(group));
          });
          return roleObj;
        }
      });
    });
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

  mapPPIParams(params: Array<any>) {
    return params.map(param => {
      return {
        items: typeToTitle(param.type)
          + ' | ' + this.ppiParents[param.conceptId]
          + ' | ' + param.name,
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

  async ppiCheck(definition: any) {
    const parents = {}
    for (const role in definition) {
      if (definition.hasOwnProperty(role)) {
        for (const group of definition[role]) {
          for (const item of group.items) {
            if (item.type !== TreeType[TreeType.PPI]) {
              continue;
            }
            for (const param of item.searchParameters) {
              const name = await this.getPPIParent(param.conceptId);
              parents[param.conceptId] = name;
            }
          }
        }
      }
    }
    return new Promise(resolve => {
      resolve(parents);
    });
  }

  async getPPIParent(conceptId: string) {
    let name;
    await this.api
      .getPPICriteriaParent(this.review.cdrVersionId, TreeType[TreeType.PPI], conceptId)
      .toPromise()
      .then(parent => name = parent.name);
    return name;
  }

  onPrint() {
    window.print();
  }
//  TODO create search param mapping functions for each for each domain/type with different format
}
