import {Component, Input, OnInit} from '@angular/core';
import {FormGroup} from '@angular/forms';
import {Subscription} from 'rxjs/Subscription';

@Component({
  selector: 'app-validator-errors',
  templateUrl: './validator-errors.component.html',
  styleUrls: ['./validator-errors.component.css']
})
export class ValidatorErrorsComponent implements OnInit {

  private static readonly errorMessages = {
    dateFormat: (params) => params.message,
    integer: (params) => params.message,
    negative: (params) => params.message,
    number: (params) => params.message,
    range: (params) => params.message,
    max: (params) => 'Max input value of ' + params.max + ' exceeded',
    maxlength: (params) => 'Max character limit of ' + params.requiredLength + ' exceeded'
  };

  @Input() form: FormGroup;
  errors = new Set();
  subscription: Subscription;

  ngOnInit(): void {
    this.subscription = this.form.valueChanges
      .subscribe(() => {
        this.errors = new Set();
        this.validateControls(this.form.controls);
      });
  }

  get showErrors() {
    return this.errors.size;
  }

  getErrors(errors: any) {
    Object.keys(errors).forEach(_type => {
      if (ValidatorErrorsComponent.errorMessages.hasOwnProperty(_type)) {
        this.errors.add(ValidatorErrorsComponent.errorMessages[_type](errors[_type]));
      }
    });
  }

  validateControls(controls: any): void {
    Object.keys(controls).forEach(key => {
      const control = controls[key];
      if (control.errors) {
        this.getErrors(control.errors);
      }
      if (control.controls) {
        this.validateControls(control.controls);
      }
    });
  }
}
