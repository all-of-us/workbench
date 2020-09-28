
// Inferred structure of JSON object coming down the wire for a date-time in Swagger. It's apparently
// not a Date as advertised.
export interface DateObjectProps {
  dayOfMonth: number;
  dayOfWeek: string;
  dayOfYear: number;
  hour: number;
  minute: number;
  month: string;
  monthValue: number;
  nano: number;
  offset: object;
  id: string;
  rules: {fixedOffset: boolean, transitions: Array<object>, transitionRules: Array<object>};
  totalSeconds: number;
  second: number;
  year: number;
}

// Convert an object as read from JSON into an actual Javascript Date.
// This works around a gap in Swagger around typescript date serialization.
export function fixSwaggerDate(swaggerWireDate: Date): Date {
  const wireObject = (swaggerWireDate as unknown) as DateObjectProps;
  return  new Date(
      wireObject.year,
      wireObject.monthValue,
      wireObject.dayOfMonth,
      wireObject.hour,
      wireObject.minute,
      wireObject.second,
      wireObject.nano / 1000);
}
