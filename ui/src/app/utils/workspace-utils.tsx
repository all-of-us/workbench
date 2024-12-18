import {
  BillingStatus,
  FeaturedWorkspaceCategory,
  Workspace,
} from 'generated/fetch';

import { serverConfigStore } from 'app/utils/stores';

export const isUsingInitialCredits = (workspace: Workspace): boolean => {
  return (
    workspace.billingAccountName ===
    'billingAccounts/' + serverConfigStore.get().config.freeTierBillingAccountId
  );
};

export const isValidBilling = (workspace: Workspace): boolean => {
  const isInitialCredits = isUsingInitialCredits(workspace);
  const enableInitialCreditsExpiration =
    serverConfigStore.get().config.enableInitialCreditsExpiration;
  return enableInitialCreditsExpiration
    ? (isInitialCredits &&
        !workspace.initialCredits.exhausted &&
        (!workspace.initialCredits.expired ||
          workspace.initialCredits.expirationBypassed)) ||
        !isInitialCredits
    : workspace.billingStatus === BillingStatus.ACTIVE;
};

export const isCommunityWorkspace = (workspace: Workspace): boolean => {
  return workspace.featuredCategory === FeaturedWorkspaceCategory.COMMUNITY;
};
