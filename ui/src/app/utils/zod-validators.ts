import { z, ZodType } from 'zod';

import { canonicalizeUrl } from './urls';

/**
 * maps null or undefined value as simplified "can't be blank" error.
 *
 * @param msg - Error message to return if blank
 * @returns
 */
const mapNoValueError = (msg?: string): z.ZodErrorMap => {
  return (issue, ctx) => {
    if (
      issue.code === 'invalid_type' &&
      (issue.received === 'null' || issue.received === 'undefined')
    ) {
      return { message: msg || "can't be blank" };
    }
    return { message: ctx.defaultError };
  };
};

// Common validation schema helpers
export const requiredString = (msg?: string): z.ZodString =>
  z
    .string({ errorMap: mapNoValueError(msg) })
    .trim()
    .min(1, { message: msg || "can't be blank" });

export const requiredStringWithMaxLength = (maximum: number, prefix = '') =>
  requiredString(`${prefix} cannot be blank`).max(maximum, {
    message: `${prefix} cannot exceed ${maximum} characters`,
  });

export const optionalStringWithMaxLength = (maximum: number, prefix = '') =>
  z
    .string()
    .max(maximum, {
      message: `${prefix} can have no more than ${maximum} characters.`,
    })
    .optional()
    .default('')
    .or(z.literal(null));

export const requiredNumber = (msg?: string): z.ZodNumber =>
  z.number({ errorMap: mapNoValueError(msg) });

export const trueBoolean = (msg: string = 'must be true') =>
  z
    .boolean({
      invalid_type_error: msg,
      required_error: msg,
    })
    .refine((val) => val === true, { message: msg });

export const nonEmptyArray = <T>(item: z.ZodType<T>, msg: string) =>
  z.array(item, { errorMap: mapNoValueError(msg) }).min(1, { message: msg });

export const httpUrl = (msg: string = 'must be a valid URL') =>
  z
    .string()
    .url({ message: msg })
    .refine(
      (val) => {
        let tryVal = val;
        // Attempt to parse the URL first, if it fails, canonicalize it
        try {
          new URL(tryVal);
        } catch {
          tryVal = canonicalizeUrl(tryVal);
        }

        try {
          const url = new URL(tryVal);
          const isHttp = url.protocol === 'http:' || url.protocol === 'https:';
          const isLocalHost =
            url.hostname === 'localhost' || url.hostname === '127.0.0.1';
          return isHttp && !isLocalHost;
        } catch {
          return false;
        }
      },
      { message: msg }
    )
    .default('');

type SuperRefineFnFromPassThru = (
  data: z.objectOutputType<{}, z.ZodTypeAny>,
  ctx: z.RefinementCtx
) => void;

/**
 * A passthrough pattern for zod validation that allows all field checks to be defined
 * in an function fn that will be evaluated without any short-circuiting that can happen
 * in the more standard zod usage patterns.  With standard zod patterns, the schema rules are
 * defined first with z.object(...), and more advanced rules that need info from more than
 *  one field come with a later .refine or .superRefine.  But those later checks will not run if
 * any of the earlier schema checks fail.  This helper allows for a pattern that avoids
 * that nasty pitfall.
 *
 * @example
 * const schema = refinedObject<MyFieldsType>((data, ctx) => {
 *     refineFields(data, ctx, {
 *      field1: requiredString("Field 1 is required"),
 *      field2: ...
 *     };
 * );
 *
 * @param fn
 * @returns
 */
export const refinedObject = <T>(
  fn: (data: T, ctx: z.RefinementCtx) => void
) => {
  return z
    .object({})
    .passthrough()
    .superRefine(fn as SuperRefineFnFromPassThru)
    .default({});
};

type FieldCheckerAll<T> = {
  [K in keyof T]: Pick<ZodType, 'safeParse'>;
};
type FieldCheckers<T> = Partial<FieldCheckerAll<T>>;

/**
 * Helper to create a list of validation checks on fields of an object.  Very helpful with
 * a refineObject usage.
 *
 * @param fields
 * @param ctx
 * @param checks
 * @returns
 */
export const refineFields = <T>(
  fields: T,
  ctx: z.RefinementCtx,
  checks: FieldCheckers<T>
): z.ZodIssue[] => {
  const issues: z.ZodIssue[] = [];

  // (type safetly enforces expected keys)
  // eslint-disable-next-line guard-for-in
  for (const f in checks) {
    const check = checks[f];

    // safeguard against undefined check which can happen with checked defined with optional?.chainging
    if (check) {
      if (!check.safeParse) {
        throw new Error(`Invalid field-checker for field ${f}`);
      }
      const result = check.safeParse(fields[f]);
      if (!result.success) {
        for (const issue of result.error.issues) {
          issues.push({
            ...issue,
            path: [f, ...issue.path],
          });
        }
      }
    }
  }
  for (const issue of issues) {
    ctx.addIssue(issue);
  }
  return issues;
};

/**
 * Helper to convert Zod validation errors into a format similar to validate.js.
 * This helps maintain compatibility with existing error handling code
 *
 * @param error
 * @returns
 */
export const formatZodErrors = (
  error: z.ZodError
): Record<string, string[]> | undefined => {
  const errors: Record<string, string[]> = {};
  let errorCount = 0;

  error.errors.forEach((err) => {
    const path = err.path.join('.');
    if (!errors[path]) {
      errors[path] = [];
    }
    errors[path].push(err.message);
    errorCount++;
  });

  // return undefined if no errors to match validate.js behavior
  return errorCount > 0 ? errors : undefined;
};

/**
 * Converts any zod errors thrown by attempt call to validate.js compatible format.
 * @param attempt
 * @returns
 */
export const zodToValidateJS = (
  attempt: () => void
): Record<string, string[]> | undefined => {
  try {
    attempt();
  } catch (error) {
    if (error instanceof z.ZodError) {
      return formatZodErrors(error);
    }
    throw error;
  }
};
