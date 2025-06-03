import { validate } from 'validate.js';

import { ResourceType } from 'generated/fetch';

import { nameValidationFormat } from 'app/utils/resources';
import { zodToValidateJS } from 'app/utils/zod-validators';
import { z } from 'zod';

export interface CopyModalFields {
  newName: string;
}

export const validateRenameModal = (
  fields: CopyModalFields,
  existingNames: string[],
  resourceType: ResourceType
): Record<string, string[]> | undefined => {
  const errors = validate(
    {
      newName: fields.newName?.trim(),
    },
    {
      newName: nameValidationFormat(existingNames, resourceType),
    }
  );
  return errors;
};

// V2 with zod ================================================================

export interface FormatChecker {
  pattern: RegExp;
  message: string;
}
const getCopyModalSchema = (format: FormatChecker, existingNames: string[]) =>
  z.object({
    newName: z
      .string()
      .trim()
      .min(1, "New name can't be blank")
      .regex(format.pattern, 'New name ' + format.message)
      .refine((val) => !existingNames.includes(val), 'New name already exists')
      .default(''),
  });

export const validateRenameModalV2 = (
  fields: CopyModalFields,
  existingNames: string[],
  resourceType: ResourceType
): Record<string, string[]> | undefined => {
  const nameValidator = nameValidationFormat(existingNames, resourceType);
  const schema = getCopyModalSchema(nameValidator.format, existingNames);

  const errors = zodToValidateJS(() => schema.parse(fields));
  return errors;
};
