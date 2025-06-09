import { validate } from 'validate.js';

import { ResourceType } from 'generated/fetch';

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
};
