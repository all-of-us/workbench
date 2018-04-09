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

import {Modifier, Operator} from 'generated';

@Component({
  selector: 'crit-modifiers',
  templateUrl: './modifiers.component.html',
  styleUrls: ['./modifiers.component.css']
})
export class ModifiersComponent implements OnInit, OnDestroy {
  existing = List();
  subscription: Subscription;
  ageAtEventMap: any;
  numOfOccurrencesMap: any;
  eventDateMap: any;
  ageAtEventMapEntries: any;

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
    this.ageAtEventMap = new Map();
    this.ageAtEventMap.set(Operator.EQUAL, 'Equal To');
    this.ageAtEventMap.set(Operator.GREATERTHAN, 'Greater Than');
    this.ageAtEventMap.set(Operator.LESSTHAN, 'Less Than');
    this.ageAtEventMapEntries = Array.of(this.ageAtEventMap.entries());
    this.numOfOccurrencesMap = new Map();
    this.numOfOccurrencesMap.set(Operator.EQUAL, 'Equal To');
    this.numOfOccurrencesMap.set(Operator.GREATERTHAN, 'Greater Than');
    this.numOfOccurrencesMap.set(Operator.LESSTHAN, 'Less Than');
    this.eventDateMap = new Map();
    this.eventDateMap.set(Operator.EQUAL, 'On');
    this.eventDateMap.set(Operator.GREATERTHAN, 'After');
    this.eventDateMap.set(Operator.LESSTHAN, 'Before');

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
