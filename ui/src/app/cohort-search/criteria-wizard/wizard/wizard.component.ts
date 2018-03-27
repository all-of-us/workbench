import {NgRedux, select} from '@angular-redux/store';
import {Component, Input, OnDestroy, OnInit, ViewEncapsulation} from '@angular/core';
import {List} from 'immutable';
import {Subscription} from 'rxjs/Subscription';

import {activeParameterList, CohortSearchActions, CohortSearchState} from '../../redux';
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
  @select(activeParameterList) selection$;

  hasSelection = false;
  subscription: Subscription;

  constructor(private actions: CohortSearchActions) {}

  ngOnInit() {
    this.subscription = this.selection$.subscribe(sel => this.hasSelection = sel.size > 0);
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

  /*
   * Navigation is prevented if there is no selection; otherwise we'd have to
   * do some checking here to make sure we don't create an empty search group
   */
  onSubmit() {
    this.actions.finishWizard();
  }
}
