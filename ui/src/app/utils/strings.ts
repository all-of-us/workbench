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
    // someday we expect to have more than two other app types
    () => {
      // join all but the last two with commas, then add 'and' before the last one
      const allButLastTwo = elements.slice(0, -2).join(', ');
      const lastTwo = elements.slice(-2).join(', and ');
      return `${allButLastTwo}, ${lastTwo}`;
    }
  );
