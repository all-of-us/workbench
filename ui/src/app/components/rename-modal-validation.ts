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

export const validateRenameModal = (
  fields: CopyModalFields,
  existingNames: string[],
  resourceType: ResourceType
): Record<string, string[]> | undefined => {
  const nameValidator = nameValidationFormat(existingNames, resourceType);
  const schema = getCopyModalSchema(nameValidator.format, existingNames);

  const errors = zodToValidateJS(() => schema.parse(fields));
  return errors;
};
