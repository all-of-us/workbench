interface DateObject {
  millis?: string;
}

/**
 * Converts a Swagger-generated date-like object into a real Date. Swagger codegen creates generic
 * objects with properties (millis etc) but no methods. As a workaround and to satisfy the TS compiler,
 * we convert those to equivalent real Date objects.
 */
export function resetDateObject(date: Date): Date {
  const obj = date as DateObject;
  return new Date(obj.millis);
}
