import {Component, OnInit} from '@angular/core';
import {FormBuilder, FormGroup, FormControl} from '@angular/forms';
import {environment} from 'environments/environment';

@Component({
  selector: 'crit-age-form',
  templateUrl: './age-form.component.html',
  styles: []
})
export class AgeFormComponent {
  ageForm: FormGroup;

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

  onSubmit() {
  }

  get debug() { return !!environment.debug; }
}
