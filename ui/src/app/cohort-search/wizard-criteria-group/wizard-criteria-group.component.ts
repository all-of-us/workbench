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
} from '../redux';

import {Criteria} from 'generated';


@Component({
  selector: 'app-wizard-criteria-group',
  templateUrl: './wizard-criteria-group.component.html',
})
export class WizardCriteriaGroupComponent {

  @select(activeCriteriaType) criteriaType$;
  @select(activeCriteriaList) criteriaList$;

  constructor(private actions: CohortSearchActions) {}

  selectionTitle(kind): string {
    if (kind === 'icd9'
        || kind === 'icd10'
        || kind === 'cpt') {
      return `Selected ${kind.toUpperCase()} Codes`;
    } else if (kind) {
      return `Selected ${kind}`;
    } else {
      return 'No Selection';
    }
  }

  typeDisplay(criteria): string {
    const name = criteria.get('name');
    const code = criteria.get('code');

    switch (criteria.get('type')) {
      case 'DEMO_GEN':
        return 'Gender';

      case 'DEMO_RACE':
        return 'Race/Ethnicity';

      case 'DEMO_AGE': case 'DEMO_DEC':
        return 'Demographic';

      default:
        return code;
    }
  }

  nameDisplay(criteria): string {
    return criteria.get('name');
  }
}
