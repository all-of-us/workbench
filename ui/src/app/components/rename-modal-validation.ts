import { validate } from 'validate.js';

import { ResourceType } from 'generated/fetch';

import { nameValidationFormat } from 'app/utils/resources';

export interface RenameModalFields {
  newName: string;
}

export const validateRenameModal = (
  fields: RenameModalFields,
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
