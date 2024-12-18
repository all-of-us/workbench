import { FeaturedWorkspaceCategory, Workspace } from 'generated/fetch';

import { serverConfigStore } from 'app/utils/stores';

export const isUsingFreeTierBillingAccount = (
  workspace: Workspace
): boolean => {
  return (
    workspace.billingAccountName ===
    'billingAccounts/' + serverConfigStore.get().config.freeTierBillingAccountId
  );
};

export const isCommunityWorkspace = (workspace: Workspace): boolean => {
  return workspace.featuredCategory === FeaturedWorkspaceCategory.COMMUNITY;
};
