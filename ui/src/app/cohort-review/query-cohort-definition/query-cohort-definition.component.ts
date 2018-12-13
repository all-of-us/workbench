import {Component, Input, OnInit} from '@angular/core';
import {CohortBuilderService, TreeType} from 'generated';
import {typeToTitle} from '../../cohort-search/utils';



@Component({
  selector: 'app-query-cohort-definition',
  templateUrl: './query-cohort-definition.component.html',
  styleUrls: ['./query-cohort-definition.component.css']
})
export class QueryCohortDefinitionComponent implements OnInit {
  definition: Array<any>;
  ppiParents: any;
  values: Array<any>;
  @Input() cohort: any;
  @Input() review: any;
  constructor(private api: CohortBuilderService) {}

  ngOnInit() {
    this.mapDefinition();
    // console.log(JSON.parse(this.cohort.criteria))
  }

  mapDefinition() {
    const definition = JSON.parse(this.cohort.criteria);
    this.ppiCheck(definition).then(parents => {
      this.ppiParents = parents;
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
        case TreeType.DRUG:
          return this.mapDrugParams(item.searchParameters);
        default:
          return this.mapParams(item.type, item.searchParameters, item.modifiers);
      }
    });
  }


// Drug
  mapDrugParams(params: Array<any>) {
    const groupedData = this.getGroupedData(params, 'group');
    const drugArray = [];
      params.map(p => {
      const typeMatched = groupedData.find( matched => matched.group === p.group.toString());
      if (typeMatched) {
        drugArray.push({
          items: typeToTitle(p.type) + ' | ' + typeMatched.customString,
          type: p.type
        });
      }
    });
     return this.removeDuplicates(drugArray);
  }


  // other than Drug
  mapParams(_type: string, params: Array<any>, mod) {
    const groupedData = this.getGroupedData(params, 'type');
    if (mod.length) {
      return this.getModifierFormattedData(groupedData, params, mod, _type);
    } else {
      return this.getOtherTreeFormattedData(groupedData, params, _type);
    }
  }

  getModifierFormattedData(groupedData, params, mod, _type) {
    const modArray =  params.map(eachParam => {
      const typeMatched = groupedData.find( matched => matched.group === eachParam.type);
      let name;
      name = mod.reduce((acc, m) => {
        const concatOperand = m.operands.reduce((final, o) => {
          return final !== '' ? `${final} & ${o}` : `${final} ${o}`;
        } , '');
        return acc !== '' ?
          `${acc} ,  ${this.operatorConversion(m.name)}
            ${this.operatorConversion(m.operator)}
            ${concatOperand}`
          :
          `${this.operatorConversion(m.name)} ${this.operatorConversion(m.operator)}
            ${concatOperand}`;
      }, '');
      return {
        items: `${typeToTitle(_type)} | ${eachParam.type} |
          ${typeMatched.customString} | ${name}`,
        type: eachParam.type
      };
    });
    return this.removeDuplicates(modArray);
  }

  getOtherTreeFormattedData(groupedData, params, _type) {

    const noModArray = params.map(param => {
      //  TODO to avoid undefined
      const typeMatched = groupedData.find( matched => matched.group === param.type);
      if (param.type === 'DEMO') {
        return {items: `${typeToTitle(_type)}
                      | ${param.type} | ${this.operatorConversion(param.subtype)} ${param.name}`,
          type: param.type};
      } else if (param.type === 'VISIT') {
        return {items: `${typeToTitle(_type)} | ${typeMatched.customString}`,
          type: param.type};
      } else {
        return _type === 'CONDITION' || _type === 'PROCEDURE' ?
          {items: `${typeToTitle(_type)} | ${param.type} | ${typeMatched.customString}`,
            type: param.type} :
          {items: `${typeToTitle(_type)} | ${typeMatched.customString}`,
            type: param.type};
      }
    });
    return this.removeDuplicates(noModArray);
  }


// to get PPI DATA
  async ppiCheck(definition: any) {
    const parents = {};
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


// utils
  getGroupedData(p, t) {
    const test = p.reduce((acc, i) => {
      const key = i[t];
      acc[key] = acc[key] || { data: []};
      acc[key].data.push(i);
      return acc;
    }, {});
    return Object.keys(test).map(k => {
      return Object.assign({}, {
        group: k,
        data: test[k].data,
        customString : test[k].data.reduce((acc, d) => {
         return this.getFormattedString(acc, d);
        }, '')
      });
    });
  }

  getFormattedString(acc, d) {
    if (d.type === 'DRUG') {
      if (d.group === false) {
        return acc === '' ? `RXNORM | ${d.value}` : `${acc}, ${d.value}`;
      } else {
        return acc === '' ? `ATC | ${d.value}` : `${acc}, ${d.value}`;
      }
    } else if (d.type === 'PM' || d.type === 'VISIT') {
      return acc === '' ? `${d.name}` : `${acc}, ${d.name}`;
    } else if (d.type === 'PPI') {
      if (d.group === false) {
        return acc === '' ? `${this.ppiParents[d.conceptId]} | ${d.name}` :
          `${acc}, ${this.ppiParents[d.conceptId]} | ${d.name}`;
      } else {
        return acc === '' ? `Parent ${this.ppiParents[d.conceptId]} | ${d.name}` :
          `${acc}, Parent ${this.ppiParents[d.conceptId]} | ${d.name}`;
      }
    } else {
      if (d.group === false) {
        return acc === '' ? d.value : `${acc}, ${d.value}`;
      } else {
        return acc === '' ? `Parent ${d.value}` : `${acc}, Parent ${d.value}`;
      }
    }
  }

  removeDuplicates(arr) {
    return arr.filter((thing, index, self) =>
      index === self.findIndex((t) => (
        t.items === thing.items && t.type === thing.type
      ))
    );
  }

  operatorConversion(operator) {
    switch (operator) {
      case 'GREATER_THAN_OR_EQUAL_TO' :
        return '>=';
      case 'LESS_THAN_OR_EQUAL_TO' :
        return '<=';
      case 'EQUAL' :
        return '=';
      case 'BETWEEN' :
        return 'Between';
      case 'ETH' :
        return 'Ethnicity';
      case 'RACE' :
        return 'Race';
      case 'AGE' :
        return 'Age';
      case 'GEN' :
        return 'Gender';
      case 'AGE_AT_EVENT' :
        return 'Age at Event';
      case 'NUM_OF_OCCURRENCES' :
        return 'Num of Occurrences';
    }
  }
}
