import {
  Component,
  OnDestroy,
  OnInit,
  ViewChild,
  ViewEncapsulation
} from '@angular/core';
import {NgRedux, select} from '@angular-redux/store';
import {Subscription} from 'rxjs/Subscription';

import {
  activeSearchGroupItemPath,
  CohortSearchActions,
  CohortSearchState,
} from '../redux';

import {Criteria} from 'generated';


@Component({
  selector: 'app-wizard-criteria-group',
  templateUrl: 'wizard-criteria-group.component.html',
  encapsulation: ViewEncapsulation.None
})
export class WizardCriteriaGroupComponent implements OnInit, OnDestroy {

  @select(s => s.getIn(activeSearchGroupItemPath(s)))
  readonly activeSearchGroupItem$;

  private subscription: Subscription;
  private criteriaType: string;
  private criteriaList;

  // TODO (jms) see below
  // @ViewChild('groupDiv') groupDiv: any;

  constructor(private ngRedux: NgRedux<CohortSearchState>,
              private actions: CohortSearchActions) {}

  ngOnInit() {
    this.subscription = this.activeSearchGroupItem$.subscribe(
      (sgi) => {
        console.dir(sgi);
        this.criteriaType = sgi.get('type');
        this.criteriaList = sgi.get('searchParameters');
        // TODO(jms) fix the scrolling bugs
        // this.groupDiv.scrollTop = this.groupDiv.scrollHeight;
    });
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

  get selection(): string {
    if (this.criteriaType === 'icd9'
        || this.criteriaType === 'icd10'
        || this.criteriaType === 'cpt') {
      return `Selected ${this.criteriaType.toUpperCase()} Codes`;
    } else if (this.criteriaType) {
      return `Selected ${this.criteriaType}`;
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

  removeCriteria(index: number) {
    const path = activeSearchGroupItemPath(this.ngRedux.getState())
      .push('searchParameters', index);
    this.actions.remove(path);
  }
}
