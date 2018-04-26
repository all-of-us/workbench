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
  modifierType = ModifierType;
  existing = List();
  subscription: Subscription;
  ageAtEventMapEntries: any;
  numOfOccurrencesMapEntries: any;
  eventDateMapEntries: any;
  ageAtEventBetween = false;
  eventDateBetween = false;

  form = new FormGroup({
    [this.modifierType.AGEATEVENT]: new FormGroup({
      operator: new FormControl(),
      value: new FormControl(),
      value1: new FormControl(),
    }),
    [this.modifierType.NUMOFOCCURRENCES]: new FormGroup({
      operator: new FormControl(),
      value: new FormControl(),
    }),
    [this.modifierType.EVENTDATE]: new FormGroup({
      operator: new FormControl(),
      value: new FormControl(),
      value1: new FormControl(),
    }),
  });

  constructor(
    private ngRedux: NgRedux<CohortSearchState>,
    private actions: CohortSearchActions,
  ) {}

  ngOnInit() {
    const ageAtEventMap = new Map([
        [Operator.GREATERTHANOREQUALTO, 'Greater Than Or Equal To'],
        [Operator.LESSTHANOREQUALTO, 'Less Than Or Equals To'],
        [Operator.BETWEEN, 'Between']
    ]);
    this.ageAtEventMapEntries = Array.from(ageAtEventMap.entries());

    const numOfOccurrencesMap = new Map([
        [Operator.EQUAL, 'Equal to'],
        [Operator.GREATERTHANOREQUALTO, 'Greater Than Or Equal To']
    ]);
    this.numOfOccurrencesMapEntries = Array.from(numOfOccurrencesMap.entries());

    const eventDateMap = new Map([
        [Operator.GREATERTHANOREQUALTO, 'Greater Than Or Equal To'],
        [Operator.LESSTHANOREQUALTO, 'Less Than Or Equals To'],
        [Operator.BETWEEN, 'Between']
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

  showHide(name) {
    const group = this.form.get(name);
    const operator = group.get('operator').value;
    if (name === ModifierType.AGEATEVENT) {
      this.ageAtEventBetween = operator === Operator.BETWEEN;
    }
    if (name === ModifierType.EVENTDATE) {
      this.eventDateBetween = operator === Operator.BETWEEN;
    }
  }

  private toModifier(name) {
    const group = this.form.get(name);
    const operator = group.get('operator').value;
    const value = group.get('value').value;
    if (value === null  || operator === null) {
      return ; // noop
    }
    const values: string[] = [];
    values.push(value);
    if (operator === Operator.BETWEEN) {
      values.push(group.get('value1').value);
    }
    return <Modifier>{
      name: this.modifierType[this.modifierType[name]],
      operator: Operator[Operator[operator]],
      operands: values
    };
  }
}
