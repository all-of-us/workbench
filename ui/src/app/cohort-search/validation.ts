import {AbstractControl, ValidatorFn} from '@angular/forms';
import * as moment from 'moment';

export function rangeValidator(name: string, min: number, max: number): ValidatorFn {
  return (control: AbstractControl): {[key: string]: any} | null => {
    const value = control.value;
    const isInt = Number.isInteger(parseFloat(value));
    const inRange = value >= min && value <= max;
    const message: any = {};
    if (!isInt) {
      message.integer = {
        message : name + ' must be a whole number'
      };
    } else if (!inRange) {
      message.range = {
        message: name + ' must be between ' + min + ' and ' + max
      };
    }
    const isValid = control.pristine || (isInt && inRange);
    return isValid ? null : message;
  };
}

export function dateValidator(): ValidatorFn {
  return (control: AbstractControl): { [key: string]: any } | null => {
    const value = control.value;
    const isValid = control.pristine || moment(value, 'YYYY-MM-DD', true).isValid();
    const message: any = {
      dateFormat: {
        message: 'Dates must be in the format \'YYYY-MM-DD\''
      }
    };
    return isValid ? null : message;
  };
}

export function validDateString(date: string, name?: string): string {
  if (!moment(date, 'YYYY-MM-DD', true).isValid()) {
    return 'Dates must be in the format \'YYYY-MM-DD\'';
  }
  return null;
}

function getControlName(control: AbstractControl) {
  const formGroup = control.parent.controls;
  return Object.keys(formGroup).find(name => control === formGroup[name]) || null;
}
