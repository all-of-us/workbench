import { switchCase } from '@terra-ui-packages/core-utils';

export const ACTION_DISABLED_INVALID_BILLING =
  'A valid billing account is required for this action';

export const NOT_ENOUGH_CHARACTERS_RESEARCH_DESCRIPTION =
  'The description you entered seems too short. Please consider ' +
  'adding more descriptive details to help the Program and your fellow Researchers ' +
  'understand your work.';

export const BASE_TITLE = 'All of Us Researcher Workbench';

export const BILLING_ACCOUNT_DISABLED_TOOLTIP =
  'You have either run out of initial credits or have an inactive billing account.';

export const oxfordCommaString = (
  elements: string[] | null | undefined
): string | null | undefined =>
  elements &&
  switchCase(
    elements.length,
    [0, () => ''],
    [1, () => elements[0]],
    [2, () => `${elements[0]} and ${elements[1]}`],
    () => {
      // join all but the last with commas, but use 'and' before the last one
      const allButLast = elements.slice(0, -1).join(', ');
      const [last] = elements.slice(-1);
      return `${allButLast}, and ${last}`;
    }
  );

export const toPascalCase = (str: string) => {
  if (!str) {
    return str;
  }
  const [fst] = str;

  return `${fst.toUpperCase()}${str.toLowerCase().slice(1)}`;
};
