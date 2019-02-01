import {Component, Input, OnInit} from '@angular/core';
import {typeToTitle} from 'app/cohort-search/utils';


@Component({
  selector: 'app-query-cohort-definition',
  templateUrl: './query-cohort-definition.component.html',
  styleUrls: ['./query-cohort-definition.component.css']
})
export class QueryCohortDefinitionComponent implements OnInit {
  definition: Array<any>;
  values: Array<any>;
  @Input() cohort: any;
  @Input() review: any;
  constructor() {}

  ngOnInit() {
    if (this.cohort) {
      this.mapDefinition();
    }
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
    return group.items.map(item => {
      return this.mapParams(item.type, item.searchParameters, item.modifiers);
    });
  }

  mapParams(_type: string, params: Array<any>, mod) {
    let groupedData;
    _type === 'DRUG' ? groupedData = this.getGroupedData(params, 'group')
      : groupedData = this.getGroupedData(params, 'type');
    if (mod.length) {
      return this.getModifierFormattedData(groupedData, params, mod, _type);
    } else {
      return this.getOtherTreeFormattedData(groupedData, params, _type);
    }
  }

  getModifierFormattedData(groupedData, params, mod, _type) {
    let typeMatched;
    const modArray =  params.map(eachParam => {
      if (eachParam.type === 'DRUG') {
        typeMatched = groupedData.find( matched => matched.group === eachParam.group.toString());
      } else {
        typeMatched = groupedData.find( matched => matched.group === eachParam.type);
      }
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
    let typeMatched;
    const noModArray = params.map(param => {
      if (param.type === 'DRUG') {
        typeMatched = groupedData.find( matched => matched.group === param.group.toString());
      } else {
        typeMatched = groupedData.find( matched => matched.group === param.type);
      }
      if (param.type === 'DEMO') {
        return {items: param.subtype === 'DEC' ? `${typeToTitle(_type)}
                      | ${param.name}` :
                      `${typeToTitle(_type)}
                      | ${this.operatorConversion(param.subtype)} | ${param.name}`,
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


// utils
  getGroupedData(p, t) {
    const groupedData = p.reduce((acc, i) => {
      const key = i[t];
      acc[key] = acc[key] || { data: []};
      acc[key].data.push(i);
      return acc;
    }, {});
    return Object.keys(groupedData).map(k => {
      return Object.assign({}, {
        group: k,
        data: groupedData[k].data,
        customString : groupedData[k].data.reduce((acc, d) => {
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
      if (!d.group) {
        return  acc === '' ? `${d.name}` :
           `${acc}, ${d.name}`;
      } else if (d.group && !d.conceptId) {
        return acc === '' ? `Survey - ${d.name}` :
          `${acc}, Survey - ${d.name}`;
      } else {
        return acc === '' ? `${d.name}` :
          `${acc}, ${d.name}`;
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
