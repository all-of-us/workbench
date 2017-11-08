import {Component, OnInit, OnDestroy, Output, EventEmitter} from '@angular/core';
import {FormGroup, FormControl, Validators} from '@angular/forms';
import {Subscription} from 'rxjs/Subscription';
import {Map} from 'immutable';

import {environment} from 'environments/environment';
import {AttributeFormComponent} from './attributes.interface';
import {Attribute} from 'generated';


function validInterval(group) {
  const start = group.get('rangeOpen').value;
  const close = group.get('rangeClose').value;
  return start <= close
    ? null
    : {'invalidRange': 'Open must precede close value'};
}

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

  @Output() attribute = new EventEmitter<Attribute>();
  @Output() submitted = new EventEmitter<boolean>();
  @Output() cancelled = new EventEmitter<boolean>();

  ngOnInit() {
    const requiredAge = Validators.compose([
      Validators.min(0),
      Validators.max(120),
      Validators.required,
    ]);

    this.sub = this.ageForm.get('operator').valueChanges.subscribe(
      (operator: string) => {
        const group = this.ageForm;
        const age = group.get('age');
        const open = group.get('rangeOpen');
        const close = group.get('rangeClose');

        if (operator === 'EQUAL') {
          open.clearValidators();
          open.updateValueAndValidity();

          close.clearValidators();
          close.updateValueAndValidity();

          age.setValidators(requiredAge);
          age.updateValueAndValidity();

        } else if (operator === 'RANGE') {
          age.clearValidators();
          age.updateValueAndValidity();

          open.setValidators(requiredAge);
          open.updateValueAndValidity();

          close.setValidators(requiredAge);
          close.updateValueAndValidity();

          group.setValidators(validInterval);
          group.updateValueAndValidity();
        }
      }
    );
  }

  ngOnDestroy() {
    this.sub.unsubscribe();
  }

  get age() { return this.ageForm.get('age'); }
  get rangeOpen() { return this.ageForm.get('rangeOpen'); }
  get rangeClose() { return this.ageForm.get('rangeClose'); }

  submit(): void {
    const operator = this.ageForm.get('operator').value;
    const operands = [];
    if (operator === 'EQUAL') {
      operands.push(this.ageForm.get('age').value);
    } else {
      operands.push(this.ageForm.get('rangeOpen').value);
      operands.push(this.ageForm.get('rangeClose').value);
    }
    const attr: Attribute = {operator, operands};
    this.attribute.emit(attr);
    this.submitted.emit(true);
  }

  cancel(): void {
    this.cancelled.emit(true);
  }

  get debug() { return !!environment.debug; }
}
