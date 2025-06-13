import { ResourceType } from 'generated/fetch';

import { nameValidationFormat } from 'app/utils/resources';
import { zodToValidateJS } from 'app/utils/zod-validators';
import { z } from 'zod';

export interface CopyModalFields {
  newName: string;
}

export interface FormatChecker {
  pattern: RegExp;
  message: string;
}
const getCopyModalSchema = (format: FormatChecker) =>
  z.object({
    newName: z
      .string()
      .trim()
      .min(1, "New name can't be blank")
      .regex(format.pattern, 'New name ' + format.message)
      .default(''),
  });

export const validateCopyModal = (
  fields: CopyModalFields,
  resourceType: ResourceType
): Record<string, string[]> | undefined => {
  const nameValidator = nameValidationFormat([], resourceType);
  const schema = getCopyModalSchema(nameValidator.format);

  const errors = zodToValidateJS(() => schema.parse(fields));
  return errors;
};
