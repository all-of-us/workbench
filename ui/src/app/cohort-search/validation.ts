import * as moment from 'moment';

export function validRange(value: number, min: number, max: number, name?: string): string {
  name = name || 'Form';
  if (value < min || value > max) {
    return name + ' cannot accept values outside range ' + min + ' - ' + max;
  }
  return null;
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
