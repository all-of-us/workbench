import {NgRedux} from '@angular-redux/store';
import {Component, OnDestroy, OnInit} from '@angular/core';
import {FormControl, FormGroup} from '@angular/forms';
import {fromJS, List} from 'immutable';
import {Subscription} from 'rxjs/Subscription';

import {
  activeModifierList,
  CohortSearchActions,
  CohortSearchState,
} from '../redux';

import {Modifier} from 'generated';

@Component({
  selector: 'crit-modifier-page',
  templateUrl: './modifier-page.component.html',
  styleUrls: ['./modifier-page.component.css']
})
export class ModifierPageComponent implements OnInit, OnDestroy {
  existing = List();
  subscription: Subscription;

  form = new FormGroup({
    ageAtEvent: new FormGroup({
      operator: new FormControl(),
      value: new FormControl(),
    }),
    numOfOccurrences: new FormGroup({
      operator: new FormControl(),
      value: new FormControl(),
    }),
    eventDate: new FormGroup({
      operator: new FormControl(),
      value: new FormControl(),
    }),
  });

  constructor(
    private ngRedux: NgRedux<CohortSearchState>,
    private actions: CohortSearchActions,
  ) {}

  ngOnInit() {
    this.form.valueChanges.subscribe(console.log);
    this.subscription = this.ngRedux
      .select(activeModifierList)
      .subscribe(mods => this.existing = mods);
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

  isSelected(name) {
    const modifier = this.toModifier(name);
    if (modifier) {
      return this.existing.includes(fromJS(modifier));
    }
  }

  select(name) {
    const modifier = this.toModifier(name);
    if (modifier) {
      this.actions.addModifier(modifier);
    }
  }

  private toModifier(name) {
    const group = this.form.get(name);
    const operator = group.get('operator').value;
    const value = group.get('value').value;
    if (value === null  || operator === null) {
      return ; // noop
    }
    return <Modifier>{name, operator, operands: [value]};
  }
}
