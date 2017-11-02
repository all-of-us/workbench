import {
  Component,
} from '@angular/core';
import {select} from '@angular-redux/store';

import {
  CohortSearchActions,
  activeGroupId,
  activeRole,
  activeCriteriaType,
  activeCriteriaList,
} from '../../redux';

import {Criteria} from 'generated';


@Component({
  selector: 'crit-selection',
  templateUrl: './selection.component.html',
})
export class SelectionComponent {

  @select(activeCriteriaType) criteriaType$;
  @select(activeCriteriaList) criteriaList$;

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

  typeDisplay(criteria): string {
    const subtype = criteria.get('subtype', '');
    const _type = criteria.get('type', '');

    if (_type.match(/^DEMO.*/i)) {
      return {
        'GEN': 'Gender',
        'RACE': 'Race/Ethnicity',
        'AGE': 'Demographic',
        'DEC': 'Demographic'
      }[subtype];
    } else {
      return criteria.get('code');
    }
  }

  nameDisplay(criteria): string {
    return criteria.get('name');
  }
}
