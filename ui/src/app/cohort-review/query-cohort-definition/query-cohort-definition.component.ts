import {Component, OnInit, Input} from '@angular/core';
 import {CohortBuilderService, TreeType} from 'generated';
 import {typeToTitle} from '../../cohort-search/utils';


@Component({
  selector: 'app-query-cohort-definition',
  templateUrl: './query-cohort-definition.component.html',
  styleUrls: ['./query-cohort-definition.component.css']
})
export class QueryCohortDefinitionComponent implements OnInit {
  // review: any;
  definition: Array<any>;
  ppiParents: any;
  @Input() cohort: any;
  @Input() review: any;
  constructor(private api: CohortBuilderService) {}

  ngOnInit() {
    this.mapDefinition();
    // console.log(JSON.parse(this.cohort.criteria))
  }

  mapDefinition() {
    const definition = JSON.parse(this.cohort.criteria)
    this.ppiCheck(definition).then(parents => {
      this.ppiParents = parents;
      // console.log(this.ppiParents);
      this.definition = ['includes', 'excludes'].map(role => {
        if (definition[role].length) {
          const roleObj = {role, groups: []};
          definition[role].forEach(group => {
            roleObj.groups.push(this.mapGroup(group));
          });
          return roleObj;
        }
      });
      // console.log(this.definition)
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
          return this.mapParams(item.type, item.searchParameters, item.modifiers);
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
          const concatOperand = m.operands.reduce((final, o) => {
            return final!=='' ? `${final} ${o}` : `${final} ${o} &`
          } , '');
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
        if(param.type === 'DEMO')
        {
          return {items:`${typeToTitle(_type)} 
                      | ${param.type} | ${param.name}`,
            type: param.type};
        }else {
          return {items:`${typeToTitle(_type)} 
                      | ${param.type} | ${param.value}  ${param.name}`,
            type: param.type};
        }
      });
    }
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


//  TODO create search param mapping functions for each for each domain/type with different format
}
