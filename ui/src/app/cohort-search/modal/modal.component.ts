import {select} from '@angular-redux/store';
import {Component, Input, OnDestroy, OnInit} from '@angular/core';
import {Map} from 'immutable';
import {Observable} from 'rxjs/Observable';
import {Subscription} from 'rxjs/Subscription';

import {
  activeCriteriaType,
  activeParameterList,
  CohortSearchActions,
  wizardOpen,
} from '../redux';
import {typeToTitle} from '../utils';

import {CRITERIA_TYPES} from '../constant';

@Component({
  selector: 'app-modal',
  templateUrl: './modal.component.html',
  styleUrls: [
    './modal.component.css',
    '../../styles/buttons.css',
  ]
})
export class ModalComponent implements OnInit, OnDestroy {
  @select(wizardOpen) open$: Observable<boolean>;
  @select(activeCriteriaType) criteriaType$: Observable<string>;
  @select(activeParameterList) selection$: Observable<any>;

  ctype: string;
  subscription;

  open = false;
  noSelection = true;
  title = '';
  mode = 'tree'; // default to criteria tree

  constructor(private actions: CohortSearchActions) {}

  ngOnInit() {
    this.subscription = this.open$
      .filter(open => !!open)
      .subscribe(_ => {
        // reset to default each time the modal is opened
        this.mode = 'tree';
        this.open = true;
      });

    this.subscription.add(this.criteriaType$
      .filter(ctype => !!ctype)
      .map(ctype => ctype.toLowerCase())
      .subscribe(ctype => {
        this.ctype = ctype;
        this.title = 'Codes';
        for (const crit of CRITERIA_TYPES) {
          const regex = new RegExp(`.*${crit.type}.*`, 'i');
          if (regex.test(this.ctype)) {
            this.title = crit.name;
          }
        }
      })
    );

    this.subscription.add(this.selection$
      .map(sel => sel.size === 0)
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

  get hasNextPage() {
    if (this.ctype === 'demo' || this.mode === 'summary') {
      return false;
    }
    return true;
  }

  nextPage() {
    if (this.mode === 'tree') {
      this.mode = 'modifiers';
    } else {
      this.mode = 'summary';
    }
  }

  /* Used to bootstrap the criteria tree */
  get rootNode() {
    return Map({
      type: this.ctype,
      id: 0,    // root parent ID is always 0
    });
  }

  get selectionTitle() {
    const title = typeToTitle(this.ctype);
    return title
      ? `Selected ${title} Codes`
      : 'No Selection';
  }
}
