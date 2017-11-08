import {Component, OnInit, OnDestroy, Output, EventEmitter} from '@angular/core';
import {FormGroup, FormControl, Validators} from '@angular/forms';
import {Subscription} from 'rxjs/Subscription';
import {Map} from 'immutable';

import {environment} from 'environments/environment';
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

const validInterval = (group: FormGroup): null|object => {
  const start = group.get('rangeOpen').value;
  const close = group.get('rangeClose').value;
  return start && close && start >= close
    ? {'invalidRange': 'The Range Open value must precede the Close value'}
    : null;
};

const requiredAgeValidator = Validators.compose([
  Validators.min(MIN_AGE),
  Validators.max(MAX_AGE),
  Validators.required,
]);

@Component({
  selector: 'crit-age-form',
  templateUrl: './age-form.component.html',
})
export class AgeFormComponent implements AttributeFormComponent, OnInit, OnDestroy {
  ageForm = new FormGroup({
    operator: new FormControl('', [
      Validators.required,
    ]),
    age: new FormControl(),
    rangeOpen: new FormControl(),
    rangeClose: new FormControl(),
  });
  private sub: Subscription;

  private readonly operators = OPERATORS;
  private readonly opCodes = Object.keys(OPERATORS);
  private readonly maxAge = MAX_AGE;
  private readonly minAge = MIN_AGE;

  @Output() attribute = new EventEmitter<Attribute>();
  @Output() submitted = new EventEmitter<boolean>();
  @Output() cancelled = new EventEmitter<boolean>();

  ngOnInit() {
    this.sub = this.operator.valueChanges.subscribe(
      (operator: string) => {
        if (_operatorIsValid(operator)) {
          console.log(this);
          if (_operatorIsRange(operator)) {
            this.age.clearValidators();
            this.age.updateValueAndValidity();

            this.rangeOpen.setValidators(requiredAgeValidator);
            this.rangeOpen.updateValueAndValidity();

            this.rangeClose.setValidators(requiredAgeValidator);
            this.rangeClose.updateValueAndValidity();

            this.ageForm.setValidators(validInterval);
            this.ageForm.updateValueAndValidity();
          } else {
            this.rangeOpen.clearValidators();
            this.rangeOpen.updateValueAndValidity();

            this.rangeClose.clearValidators();
            this.rangeClose.updateValueAndValidity();

            this.age.setValidators(requiredAgeValidator);
            this.age.updateValueAndValidity();
          }
        }
      }
    );
  }

  ngOnDestroy() {
    this.sub.unsubscribe();
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
    const operator = this.operator.value;
    const operands = this.operatorIsRange
      ? [this.rangeOpen.value, this.rangeClose.value]
      : [this.age.value];
    const attr: Attribute = {operator, operands};
    this.attribute.emit(attr);
    this.submitted.emit(true);
  }

  cancel(): void {
    this.cancelled.emit(true);
  }

  get debug() { return !!environment.debug; }
}
