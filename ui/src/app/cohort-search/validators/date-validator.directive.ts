import {Directive} from '@angular/core';
import {FormControl, NG_VALIDATORS, ValidationErrors, Validator} from '@angular/forms';

import * as moment from 'moment';

@Directive({
  selector: '[appDateValidator]',
  providers: [{provide: NG_VALIDATORS, useExisting: DateValidatorDirective, multi: true}]
})
export class DateValidatorDirective implements Validator {

  validate(control: FormControl): ValidationErrors {
    const dateString = control.value;
    const isValid = control.pristine || moment(dateString, 'YYYY-MM-DD', true).isValid();
    const message = {
      dateFormat: {
        message: 'Dates must be in the format \'YYYY-MM-DD\''
      }
    };
    return isValid ? null : message;
  }

}
