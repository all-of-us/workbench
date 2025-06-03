import { z } from 'zod';
import { validate } from 'validate.js';
import { ResourceType } from 'generated/fetch';
import { zodToValidateJS } from "app/utils/zod-validators";
import { nameValidationFormat } from 'app/utils/resources';

export interface CopyModalFields {
    newName: string;
}

export const validateCopyModal = (
  fields: CopyModalFields,
  resourceType: ResourceType
): Record<string, string[]> | undefined => {
    const errors = validate(
        {
          newName: fields.newName?.trim(),
        },
        {
          newName: nameValidationFormat([], resourceType),
        }
      );
      return errors;
}

// V2 with zod ================================================================

export interface FormatChecker {
    pattern: RegExp;
    message: string;
}
const getCopyModalSchema = (format: FormatChecker) => z.object({
    newName: z.string().trim().min(1, "New name can't be blank")
      .regex(format.pattern, 'New name ' + format.message)
      .default(''),
  });

export const validateCopyModalV2 = (
  fields: CopyModalFields, 
  resourceType: ResourceType
): Record<string, string[]> | undefined => {
  const nameValidator = nameValidationFormat([], resourceType)
  const schema = getCopyModalSchema(nameValidator.format);

  const errors = zodToValidateJS(() => schema.parse(fields));
  return errors;
};