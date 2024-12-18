import { FeaturedWorkspaceCategory, Workspace } from 'generated/fetch';

import { serverConfigStore } from 'app/utils/stores';

export const isUsingInitialCredits = (workspace: Workspace): boolean => {
  return (
    workspace.billingAccountName ===
    'billingAccounts/' + serverConfigStore.get().config.freeTierBillingAccountId
  );
};
export const isValidBilling = (workspace: Workspace): boolean => {
  const isInitialCredits = isUsingInitialCredits(workspace);
  return (
    (isInitialCredits &&
      !workspace.initialCredits.exhausted &&
      !workspace.initialCredits.expired) ||
    !isInitialCredits
  );
};

export const isCommunityWorkspace = (workspace: Workspace): boolean => {
  return workspace.featuredCategory === FeaturedWorkspaceCategory.COMMUNITY;
};
