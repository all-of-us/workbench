import {AbstractControl, ValidatorFn} from '@angular/forms';
import * as moment from 'moment';

export function validRange(value: number, min: number, max: number, name?: string): string {
  name = name || 'Form';
  if (value < min || value > max) {
    return name + ' cannot accept values outside range ' + min + ' - ' + max;
  }
  return null;
}

export function rangeValidator(min: number, max: number): ValidatorFn {
  return (control: AbstractControl): {[key: string]: any} | null => {
    console.log(control);
    const value = control.value;
    const isInt = Number.isInteger(parseFloat(value));
    const inRange = value >= min && value <= max;
    const message: any = {};
    if (!isInt) {
      message.integer = {message : 'Value must be a whole number'};
    } else if (!inRange) {
      message.range = {message: 'Value must be between ' + min + ' and ' + max};
    }
    const isValid = control.pristine || (isInt && inRange);
    return isValid ? null : message;
  };
}


export function validInteger(value: string, name?: string): string {
  name = name || 'Form';
  if (!Number.isInteger(parseFloat(value))) {
    return name + ' can only accept valid integers';
  }
  return null;
}

export function validDateString(date: string, name?: string): string {
  if (!moment(date, 'YYYY-MM-DD', true).isValid()) {
    return 'Dates must be in the format \'YYYY-MM-DD\'';
  }
  return null;
}
