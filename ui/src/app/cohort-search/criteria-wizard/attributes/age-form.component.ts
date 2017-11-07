import {Component, Output, EventEmitter} from '@angular/core';
import {FormBuilder, FormGroup, FormControl} from '@angular/forms';

import {environment} from 'environments/environment';
import {AttributeFormComponent} from './attributes.interface';
import {Attribute} from 'generated';

@Component({
  selector: 'crit-age-form',
  templateUrl: './age-form.component.html',
  styles: []
})
export class AgeFormComponent implements AttributeFormComponent {
  ageForm: FormGroup;
  @Output() attribute = new EventEmitter<Attribute>();
  @Output() submitted = new EventEmitter<boolean>();
  @Output() cancelled = new EventEmitter<boolean>();

  constructor(private formBuilder: FormBuilder) {
    this.createForm();
  }

  createForm() {
    this.ageForm = this.formBuilder.group({
      operator: '',
      age: '',
      rangeOpen: '',
      rangeClose: '',
    });
  }

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
