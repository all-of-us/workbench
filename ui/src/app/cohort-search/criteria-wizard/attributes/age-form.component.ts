import {Component, OnInit, OnDestroy, Output, EventEmitter} from '@angular/core';
import {FormGroup, FormControl, Validators} from '@angular/forms';
import {select} from '@angular-redux/store';
import {Subscription} from 'rxjs/Subscription';
import {List, Map, fromJS} from 'immutable';

import {environment} from 'environments/environment';
import {activeParameterList} from '../../redux';
import {AttributeFormComponent} from './attributes.interface';
import {Attribute} from 'generated';

const OPERATORS = {
  'RANGE': 'In Range',
  'EQ': 'Equal To',
  'GT': 'Greater Than',
  'LT': 'Less Than',
  'GTE': 'Greater Than or Equal To',
  'LTE': 'Less Than or Equal To',
};

const MAX_AGE = 120;
const MIN_AGE = 0;
const _operatorIsRange = (op: string): boolean => (op === 'RANGE');
const _operatorIsValid = (op: string): boolean => (Object.keys(OPERATORS).includes(op));

const _asAttribute = (ageForm: FormGroup): Attribute => {
  const operator = ageForm.get('operator').value;
  const operands = _operatorIsRange(operator)
    ? [ageForm.get('rangeOpen').value, ageForm.get('rangeClose').value]
    : [ageForm.get('age').value];
  return <Attribute>{operator, operands};
};

const _validate = {
  interval: (group: FormGroup): null|object => {
    const start = group.get('rangeOpen').value;
    const close = group.get('rangeClose').value;
    return start && close && start >= close
      ? {invalidRange: 'The Range Open value must precede the Close value'}
      : null;
  },
  uniqueness: (existent: List<string>) =>
    (ageForm: FormGroup): null|object => {
      const wrapped = List([fromJS(_asAttribute(ageForm))]);
      const hash = wrapped.hashCode();
      const paramId = `param${hash}`;
      return existent.includes(paramId)
        ? {duplicateAttribute: 'This Criterion has already been selected'}
        : null;
  },
  age: Validators.compose([
    Validators.min(MIN_AGE),
    Validators.max(MAX_AGE),
    Validators.required,
  ]),
};

@Component({
  selector: 'crit-age-form',
  templateUrl: './age-form.component.html',
})
export class AgeFormComponent implements AttributeFormComponent, OnInit, OnDestroy {
  @select(activeParameterList) parameterSelection$;
  private selected: List<string>;

  ageForm = new FormGroup({
    operator: new FormControl('', [
      Validators.required,
    ]),
    age: new FormControl(),
    rangeOpen: new FormControl(),
    rangeClose: new FormControl(),
  });
  private subscription: Subscription;

  private readonly operators = OPERATORS;
  private readonly opCodes = Object.keys(OPERATORS);
  private readonly maxAge = MAX_AGE;
  private readonly minAge = MIN_AGE;

  @Output() attribute = new EventEmitter<Attribute>();
  @Output() submitted = new EventEmitter<boolean>();
  @Output() cancelled = new EventEmitter<boolean>();

  ngOnInit() {
    this.subscription = this.parameterSelection$
      .map(params => params.map(n => n.get('parameterId')))
      .subscribe(ids => this.selected = ids);

    const [isRange, isNotRange] = this.operator.valueChanges
      .filter(_operatorIsValid)
      .partition(_operatorIsRange);

    this.subscription.add(isRange.subscribe(op => {
      this.age.clearValidators();
      this.age.updateValueAndValidity();

      this.rangeOpen.setValidators(_validate.age);
      this.rangeOpen.updateValueAndValidity();

      this.rangeClose.setValidators(_validate.age);
      this.rangeClose.updateValueAndValidity();

      this.ageForm.setValidators([
        _validate.interval,
        _validate.uniqueness(this.selected),
      ]);
      this.ageForm.updateValueAndValidity();
    }));

    this.subscription.add(isNotRange.subscribe(op => {
      this.rangeOpen.clearValidators();
      this.rangeOpen.updateValueAndValidity();

      this.rangeClose.clearValidators();
      this.rangeClose.updateValueAndValidity();

      this.age.setValidators(_validate.age);
      this.age.updateValueAndValidity();

      this.ageForm.setValidators([
        _validate.uniqueness(this.selected),
      ]);
      this.ageForm.updateValueAndValidity();
    }));
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

  get operator() { return this.ageForm.get('operator'); }
  get age() { return this.ageForm.get('age'); }
  get rangeOpen() { return this.ageForm.get('rangeOpen'); }
  get rangeClose() { return this.ageForm.get('rangeClose'); }

  get operatorIsRange() { return _operatorIsRange(this.operator.value); }
  get operatorIsValid() { return _operatorIsValid(this.operator.value); }

  get errorList() {
    const _errors = this.ageForm.errors;
    if (_errors) {
      return Object.keys(_errors).map(key => _errors[key]);
    }
  }

  submit(): void {
    this.attribute.emit(_asAttribute(this.ageForm));
    this.submitted.emit(true);
  }

  cancel(): void {
    this.cancelled.emit(true);
  }

  get debug() { return !!environment.debug; }
}
