import {Component} from '@angular/core';
import {select} from '@angular-redux/store';
import {List} from 'immutable';

import {
  CohortSearchActions,
  activeGroupId,
  activeRole,
  activeCriteriaType,
  activeParameterList,
} from '../../redux';

import {Criteria} from 'generated';


@Component({
  selector: 'crit-selection',
  templateUrl: './selection.component.html',
  styleUrls: ['./selection.component.css'],
})
export class SelectionComponent {

  @select(activeCriteriaType) criteriaType$;
  @select(activeParameterList) criteriaList$;

  constructor(private actions: CohortSearchActions) {}

  get selectionTitle$() {
    return this.criteriaType$.map(kind => {
      kind = kind || '';
      if (kind.match(/^(ICD|CPT).*/i)) {
        return `Selected ${kind.toUpperCase()} Codes`;
      } else if (kind.match(/^DEMO.*/i)) {
        return 'Selected Demographics Codes';
      } else {
        return 'No Selection';
      }
    });
  }

  remove(parameter): void {
    this.actions.removeParameter(parameter.get('parameterId'));
  }

  typeDisplay(parameter): string {
    const subtype = parameter.get('subtype', '');
    const _type = parameter.get('type', '');

    if (_type.match(/^DEMO.*/i)) {
      return {
        'GEN': 'Gender',
        'RACE': 'Race/Ethnicity',
        'AGE': 'Age',
        'DEC': 'Deceased'
      }[subtype];
    } else {
      return parameter.get('code');
    }
  }

  nameDisplay(parameter): string {
    const subtype = parameter.get('subtype', '');
    const _type = parameter.get('type', '');
    if (_type.match(/^DEMO.*/i) && subtype.match(/AGE|DEC/i)) {
      return '';
    }
    return parameter.get('name');
  }

  attributeDisplay(parameter): string {
    const attr = parameter.get('attribute', '');

    const kind = `${parameter.get('type', '')}${parameter.get('subtype', '')}`;
    if (kind.match(/^DEMO.*AGE/i)) {
      const op = {
        'between': 'In Range',
        '=': 'Equal To',
        '>': 'Greater Than',
        '<': 'Less Than',
        '>=': 'Greater Than or Equal To',
        '<=': 'Less Than or Equal To',
      }[attr.get('operator')];
      const args = attr.get('operands', List()).join(', ');
      return `${op} ${args}`;
    }
  }
}
