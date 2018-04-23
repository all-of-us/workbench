import {select} from '@angular-redux/store';
import {Component, Input, OnInit} from '@angular/core';
import {Observable} from 'rxjs/Observable';
import {Subscription} from 'rxjs/Subscription';

import {
  activeCriteriaType,
  activeParameterList,
  CohortSearchActions,
  wizardOpen,
} from '../redux';

import {CRITERIA_TYPES} from '../constant';

@Component({
  selector: 'app-modal',
  templateUrl: './modal.component.html',
  styleUrls: [
    './modal.component.css',
    '../../styles/buttons.css',
  ]
})
export class ModalComponent {
  @select(wizardOpen) open$: Observable<boolean>;
  @select(activeCriteriaType) criteriaType$: Observable<string>;
  @select(activeParameterList) selection$;

  ctype: string;
  subscription;

  open = false;
  noSelection = true;
  title = '';

  constructor(private actions: CohortSearchActions) {}

  ngOnInit() {
    this.subscription = this.open$
      .filter(open => !!open)
      .subscribe(open => this.open = open);

    this.subscription.add(this.criteriaType$
      .filter(ctype => !!ctype)
      .map(ctype => ctype.toLowerCase())
      .subscribe(ctype => {
        this.ctype = ctype;
        this.title = 'Codes';
        for (let crit of CRITERIA_TYPES) {
          const regex = new RegExp(`.*${crit.type}.*`, 'i');
          if (regex.test(this.ctype)) {
            this.title = crit.name;
          }
        }
      })
    );

    this.subscription.add(this.selection$
      .map(sel => sel.size == 0)
      .subscribe(sel => this.noSelection = sel)
    );
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

  cancel() {
    this.open = false;
    this.actions.cancelWizard();
  }

  finish() {
    this.open = false;
    this.actions.finishWizard();
  }
}
