import { CalendarValueType } from 'primereact/calendar';

import { isBlank } from './index';

export const MILLIS_PER_DAY = 24 * 60 * 60 * 1000;
export const getWholeDaysFromNow = (timeInMillis: number): number =>
  Math.floor((timeInMillis - Date.now()) / MILLIS_PER_DAY);
export const plusDays = (date: number, days: number): number =>
  date + MILLIS_PER_DAY * days;
export const nowPlusDays = (days: number) => plusDays(Date.now(), days);

// To convert datetime strings into human-readable dates
export function displayDate(time: number): string {
  const date = new Date(time);
  // datetime formatting to slice off weekday from readable date string
  return date.toLocaleString('en-US', {
    year: '2-digit',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: true,
  });
}

export function displayDateWithoutHours(time: number): string {
  if (!time) {
    return null;
  }
  const date = new Date(time);
  // datetime formatting to slice off weekday and exact time
  return date.toLocaleString('en-us', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  });
}

// If the time passed is null, return the nullDateStringRep else format it into date without hours
export function formatDate(
  timeInEpochMillis: number | undefined,
  nullDateStringRep?: string
): string {
  return displayDateWithoutHours(timeInEpochMillis) || nullDateStringRep;
}

export function isDateValid(date: Date): boolean {
  return (
    date &&
    typeof date === 'object' &&
    !isBlank(date.toString()) &&
    !isNaN(date.valueOf())
  );
}

// CalendarValueType is a union type of Date | Date[] | string | undefined | null
// if Date or string: return a Date
// if Date[], undefined, or null: return undefined
export const maybeToSingleDate = (c: CalendarValueType): Date | undefined => {
  // can't use cond() here because type narrowing isn't supported
  if (c instanceof Date) {
    return c;
  }
  if (typeof c === 'string') {
    return new Date(c);
  }
  return undefined;
};
