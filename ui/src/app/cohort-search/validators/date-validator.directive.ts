import {Directive} from '@angular/core';
import {FormControl, ValidationErrors} from '@angular/forms';

import * as moment from 'moment';

@Directive({
  selector: '[appDateValidator]'
})
export class DateValidatorDirective {

  validate(form: FormControl): ValidationErrors {
    const dateString = form.value;
    const isValid = moment(dateString, 'YYYY-MM-DD', true).isValid();
    const message = {
      format: {
        message: 'Dates must be in the format \'YYYY-MM-DD\''
      }
    };
    return isValid ? null : message;
  }

}
