import {
  Component,
  ViewEncapsulation
} from '@angular/core';
import {NgRedux, select} from '@angular-redux/store';

import {
  CohortSearchActions,
  CohortSearchState,
  activeItemId,
  activeGroupId,
  activeRole,
  activeCriteriaType,
  activeCriteriaList,
} from '../redux';

import {Criteria} from 'generated';


@Component({
  selector: 'app-wizard-criteria-group',
  templateUrl: 'wizard-criteria-group.component.html',
  encapsulation: ViewEncapsulation.None
})
export class WizardCriteriaGroupComponent {

  @select(activeCriteriaType) criteriaType$;
  @select(activeCriteriaList) criteriaList$;

  constructor(private ngRedux: NgRedux<CohortSearchState>,
              private actions: CohortSearchActions) {}

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
        return `Gender-${name}`;

      case 'DEMO_RACE':
        return `Race/Ethnicity-${name}`;

      case 'DEMO_AGE': case 'DEMO_DEC':
        return name;

      default:
        return `${code}-${name}`;
    }
  }

  removeCriterion(criterionId: number) {
    const state = this.ngRedux.getState();
    this.actions.removeCriterion(
      activeRole(state),
      activeGroupId(state),
      activeItemId(state),
      criterionId
    );
  }
}
