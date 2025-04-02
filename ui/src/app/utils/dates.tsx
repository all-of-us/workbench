import { isBlank } from './index';

export const MILLIS_PER_DAY = 24 * 60 * 60 * 1000;
export const ONE_YEAR = 365 * MILLIS_PER_DAY;
export const getWholeDaysFromNow = (timeInMillis: number): number =>
  Math.floor((timeInMillis - Date.now()) / MILLIS_PER_DAY);
export const plusDays = (date: number, days: number): number =>
  date + MILLIS_PER_DAY * days;
export const minusDays = (date: number, days: number): number =>
  plusDays(date, -days);
export const nowPlusDays = (days: number) => plusDays(Date.now(), days);

// To convert datetime strings into human-readable dates in the format
// 08/11/23, 11:26 AM
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

// To convert datetime strings into human-readable dates in the format
// Feb 14, 2022
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

// if Date or string: return a Date
// if Date[], undefined, or null: return undefined
export const maybeToSingleDate = (
  c: Date | Date[] | string | null
): Date | undefined => {
  // can't use cond() here because type narrowing isn't supported
  if (c instanceof Date) {
    return c;
  }
  if (typeof c === 'string') {
    return new Date(c);
  }
  return undefined;
};

/**
 * Converts a local date-time string (in format YYYY-MM-DDThh:mm) to epoch milliseconds
 * @param localDateTime String in format YYYY-MM-DDThh:mm
 * @returns Epoch time in milliseconds, or null if the input is falsy
 */
export function convertLocalDateTimeToEpochMillis(
  localDateTime: string
): number | null {
  if (!localDateTime) {
    return null;
  }
  const [datePart, timePart] = localDateTime.split('T');
  const [year, month, day] = datePart.split('-').map(Number);
  const [hours, minutes] = timePart.split(':').map(Number);
  const localDate = new Date(year, month - 1, day, hours, minutes);
  return localDate.getTime();
}

/**
 * Formats epoch milliseconds to a local date-time string in format YYYY-MM-DDThh:mm
 * @param timestamp Epoch time in milliseconds
 * @returns Formatted date-time string, or empty string if the input is falsy
 */
export function formatDateTimeLocal(
  timestamp: number | null | undefined
): string {
  if (!timestamp) {
    return '';
  }
  const date = new Date(timestamp);

  // Format as YYYY-MM-DDThh:mm in local timezone
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');

  return `${year}-${month}-${day}T${hours}:${minutes}`;
}
