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
  // const isExpired =
  //   workspace?.initialCredits.expirationEpochMillis < Date.now();
  const isExpired = false;
  return enableInitialCreditsExpiration
    ? !isInitialCredits ||
        (isInitialCredits &&
          !workspace.initialCredits.exhausted &&
          (!isExpired || workspace.initialCredits.expirationBypassed))
    : workspace.billingStatus === BillingStatus.ACTIVE;
};

export const isCommunityWorkspace = (workspace: Workspace): boolean => {
  return workspace.featuredCategory === FeaturedWorkspaceCategory.COMMUNITY;
};
