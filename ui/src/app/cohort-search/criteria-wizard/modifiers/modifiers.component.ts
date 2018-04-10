import {NgRedux} from '@angular-redux/store';
import {Component, OnDestroy, OnInit} from '@angular/core';
import {FormControl, FormGroup} from '@angular/forms';
import {fromJS, List} from 'immutable';
import {Subscription} from 'rxjs/Subscription';

import {
  activeModifierList,
  CohortSearchActions,
  CohortSearchState,
} from '../../redux';

import {Modifier, ModifierType, Operator} from 'generated';

@Component({
  selector: 'crit-modifiers',
  templateUrl: './modifiers.component.html',
  styleUrls: ['./modifiers.component.css']
})
export class ModifiersComponent implements OnInit, OnDestroy {
  modifierType: ModifierType;
  existing = List();
  subscription: Subscription;
  ageAtEventMapEntries: any;
  numOfOccurrencesMapEntries: any;
  eventDateMapEntries: any;

  form = new FormGroup({
    AGE_AT_EVENT: new FormGroup({
      operator: new FormControl(),
      value: new FormControl(),
    }),
    NUM_OF_OCCURRENCES: new FormGroup({
      operator: new FormControl(),
      value: new FormControl(),
    }),
    EVENT_DATE: new FormGroup({
      operator: new FormControl(),
      value: new FormControl(),
    }),
  });

  constructor(
    private ngRedux: NgRedux<CohortSearchState>,
    private actions: CohortSearchActions,
  ) {}

  ngOnInit() {
    const ageAtEventMap = new Map([
        [Operator.EQUAL, 'Equal To'],
        [Operator.GREATERTHAN, 'Greater Than'],
        [Operator.LESSTHAN, 'Less Than']
    ]);
    this.ageAtEventMapEntries = Array.from(ageAtEventMap.entries());

    const numOfOccurrencesMap = new Map([
        [Operator.EQUAL, 'Equal To'],
        [Operator.GREATERTHAN, 'Greater Than'],
        [Operator.LESSTHAN, 'Less Than']
    ]);
    this.numOfOccurrencesMapEntries = Array.from(numOfOccurrencesMap.entries());

    const eventDateMap = new Map([
        [Operator.EQUAL, 'On'],
        [Operator.GREATERTHAN, 'After'],
        [Operator.LESSTHAN, 'Before']
    ]);
    this.eventDateMapEntries = Array.from(eventDateMap.entries());

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
    return <Modifier>{name: ModifierType.AGEATEVENT, operator: Operator.EQUAL, operands: [value]};
  }
}
