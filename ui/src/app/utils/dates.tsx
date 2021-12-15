import {isBlank} from './index';

export const MILLIS_PER_DAY = 24 * 60 * 60 * 1000;
export const daysFromNow = (timeInMillis: number): number => Math.floor((timeInMillis - Date.now()) / MILLIS_PER_DAY);

// To convert datetime strings into human-readable dates
export function displayDate(time: number): string {
  const date = new Date(time);
  // datetime formatting to slice off weekday from readable date string
  return date.toLocaleString('en-US',
    {
      year: '2-digit', month: '2-digit', day: '2-digit',
      hour: '2-digit', minute: '2-digit', hour12: true
    });
}

export function displayDateWithoutHours(time: number): string {
  const date = new Date(time);
  // datetime formatting to slice off weekday and exact time
  return date.toLocaleString('en-us', {month: 'short', day: 'numeric', year: 'numeric'});
}

export function isDateValid(date: Date): boolean {
  return date && typeof date === 'object' && !isBlank(date.toString()) && !isNaN(date.valueOf());
}
