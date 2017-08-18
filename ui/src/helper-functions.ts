interface DateObject {
  millis?: string;
}
export function resetDateObject(date: Date): Date {
  const obj = date as DateObject;
  return new Date(obj.millis);
}
