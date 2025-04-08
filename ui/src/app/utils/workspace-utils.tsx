import { FeaturedWorkspaceCategory, Workspace } from 'generated/fetch';

import { serverConfigStore } from 'app/utils/stores';

export const EARLIEST_PUBLIC_CDR_VERSION_NUMBER_INCLUDING_AIAN = 8;

export const isUsingInitialCredits = (workspace: Workspace): boolean => {
  return (
    workspace.billingAccountName ===
    'billingAccounts/' +
      serverConfigStore.get().config.initialCreditsBillingAccountId
  );
};

export const isValidBilling = (workspace: Workspace): boolean => {
  const isInitialCredits = isUsingInitialCredits(workspace);
  const enableInitialCreditsExpiration =
    serverConfigStore.get().config.enableInitialCreditsExpiration;
  const isExpired =
    enableInitialCreditsExpiration &&
    workspace?.initialCredits.expirationEpochMillis < Date.now();
  return (
    !isInitialCredits ||
    (isInitialCredits &&
      !workspace.initialCredits.exhausted &&
      (!isExpired || workspace.initialCredits.expirationBypassed))
  );
};

export const isCommunityWorkspace = (workspace: Workspace): boolean => {
  return workspace.featuredCategory === FeaturedWorkspaceCategory.COMMUNITY;
};

export const showAIANResearchPurpose = (
  publicCDRReleaseNumber: number
): boolean =>
  publicCDRReleaseNumber >= EARLIEST_PUBLIC_CDR_VERSION_NUMBER_INCLUDING_AIAN;
