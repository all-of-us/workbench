import {Component, OnInit, OnDestroy, ViewChild} from '@angular/core';
import {NgRedux, select} from '@angular-redux/store';
import {Subscription} from 'rxjs/Subscription';

import {CohortSearchActions} from '../actions';
import {CohortSearchState, getActiveSGIPath} from '../store';

import {Criteria} from 'generated';


@Component({
  selector: 'app-wizard-criteria-group',
  templateUrl: 'wizard-criteria-group.component.html',
})
export class WizardCriteriaGroupComponent implements OnInit, OnDestroy {

  @select(s => s.getIn(getActiveSGIPath(s).unshift('search')))
  readonly activeSearchgroup$;

  private subscription: Subscription;
  private criteriaType: string;
  private criteriaList;

  // @ViewChild('groupDiv') groupDiv: any;

  constructor(private ngRedux: NgRedux<CohortSearchState>,
              private actions: CohortSearchActions) {}

  ngOnInit() {
    this.subscription = this.activeSearchgroup$.subscribe(
      (sgi) => {
        console.dir(sgi);
        this.criteriaType = sgi.get('type');
        this.criteriaList = sgi.get('searchParameters').toJS();
        // fires whenever the crit list is updated
        // this.groupDiv.scrollTop = this.groupDiv.scrollHeight;
    });
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

  removeCriteria(index: number) {
    const path = getActiveSGIPath(this.ngRedux.getState())
      .push('searchParameters', index);
    this.actions.removeCriteria(path);
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }
}
