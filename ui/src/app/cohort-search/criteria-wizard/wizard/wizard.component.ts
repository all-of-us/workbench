import {NgRedux} from '@angular-redux/store';
import {Component, Input, OnDestroy, OnInit, ViewEncapsulation} from '@angular/core';
import {Subscription} from 'rxjs/Subscription';

import {
  activeParameterList,
  CohortSearchActions,
  CohortSearchState,
} from '../../redux';
import {typeToTitle} from '../../utils';

@Component({
  selector: 'app-criteria-wizard',
  templateUrl: './wizard.component.html',
  styleUrls: [
    './wizard.component.css',
    '../../../styles/buttons.css',
  ],
  encapsulation: ViewEncapsulation.None,
})
export class WizardComponent implements OnInit, OnDestroy {
  @Input() open: boolean;
  @Input() criteriaType: string;
  disableFinish = true;
  subscription: Subscription;

  constructor(
    private ngRedux: NgRedux<CohortSearchState>,
    private actions: CohortSearchActions,
  ) {}

  ngOnInit() {
    this.subscription = this.ngRedux
      .select(activeParameterList)
      .map(list => !(list.size > 0))
      .subscribe(val => this.disableFinish = val);
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

  get critPageTitle() {
    return `Choose ${typeToTitle(this.criteriaType)} Codes`;
  }

  onCancel() {
    this.actions.cancelWizard();
  }

  onSubmit() {
    if (this.disableFinish) {
      /*
       * If there are no selected criteria, then we cancel to revert to the
       * pre-wizard state, not finish.
       */
      return this.onCancel();
    }
    this.actions.finishWizard();
  }
}
