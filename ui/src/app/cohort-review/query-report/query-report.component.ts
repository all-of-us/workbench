import {Component, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';

import {TreeSubType, TreeType} from 'generated';
import {subtypeToTitle, typeToTitle} from '../../cohort-search/utils';
import * as moment from "moment";


@Component({
  selector: 'app-query-report',
  templateUrl: './query-report.component.html',
  styleUrls: ['./query-report.component.css']
})
export class QueryReportComponent implements OnInit {
  cohort: any;
  review: any;
  definition: Array<any>;
  testing: any;
  test: any;
  duplicateItems = [];
  constructor(private route: ActivatedRoute) {}

  ngOnInit() {
    const {cohort, review} = this.route.snapshot.data;
    this.cohort = cohort;
    this.review = review;
    this.test= JSON.parse(this.cohort.criteria);
    // console.log(cohort.criteria);
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
     console.log( this.definition)
  }

  mapGroup(group: any) {
    return group.items.map(item => {
      switch (item.type) {
        case TreeType.PM:
          return this.mapPMParams(item.searchParameters);
        default:
          return this.mapParams(item.type, item.searchParameters, item.modifiers);
      }
    });
  }

  mapPMParams(params: Array<any>) {
    return params.map(param => {
       return {items: typeToTitle(param.type) + ' | ' + param.name, type:param.type};
    });
  }

  removeUnderScoreLowerCase(name: string) {
    return name.replace(/_/g, " ").toLowerCase();
  }

  operatorConversion(operator){
    switch (operator) {
      case 'GREATER_THAN_OR_EQUAL_TO':
        return '>=';
      case 'LESS_THAN_OR_EQUAL_TO':
        return '<=';
      case 'EQUAL':
        return '=';
      case 'BETWEEN':
        return 'between';
    }
  }

  mapParams(_type: string, params: Array<any>, mod) {
    if(mod.length > 0) {
       return params.map(eachParam => {
         let name;
         name = mod.reduce((acc, m) => {
           const concatOperand = m.operands.reduce((final, o) => `${final} ${o}`, '');
           return acc !== '' ?
             `${acc} ,  ${this.removeUnderScoreLowerCase(m.name)} 
              ${this.operatorConversion(m.operator)} 
               ${concatOperand}`
             :
             `${this.removeUnderScoreLowerCase(m.name)} 
              ${this.operatorConversion(m.operator)} 
                ${concatOperand}`;
         }, '');
         return {
           items: `${typeToTitle(_type)} | 
                    ${eachParam.type} | ${eachParam.value} 
                    ${eachParam.name} | ${name}`,
           type: eachParam.type
         };
      });
    } else {
      return params.map(param => {
        return {items:`${typeToTitle(_type)} 
                      | ${param.type} | ${param.value}  ${param.name}`,
                type: param.type};
      });
    }
  }


  onPrint() {
    window.print();
  }
//  TODO create search param mapping functions for each for each domain/type with different format
}
