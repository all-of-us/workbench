import {Directive} from '@angular/core';
import {FormControl, NG_VALIDATORS, ValidationErrors, Validator} from '@angular/forms';

@Directive({
  selector: '[appIntegerValidator]',
  providers: [{provide: NG_VALIDATORS, useExisting: IntegerValidatorDirective, multi: true}]
})
export class IntegerValidatorDirective implements Validator {

  validate(control: FormControl): ValidationErrors {
    console.log(control);
    const value = control.value;
    const isValid = control.pristine || Number.isInteger(parseFloat(value));
    const message = {
      integer: {
        message: 'Value must be a whole number'
      }
    };
    return isValid ? null : message;
  }

}

