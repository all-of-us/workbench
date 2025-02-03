import { FeaturedWorkspaceCategory, Workspace } from 'generated/fetch';

import { serverConfigStore } from 'app/utils/stores';

export const EARLIEST_PUBLIC_CDR_VERSION_NUMBER_INCLUDING_AIAN = 8;

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

export const showAIANResearchPurpose = (
  publicCDRReleaseNumber: number
): boolean =>
  publicCDRReleaseNumber >= EARLIEST_PUBLIC_CDR_VERSION_NUMBER_INCLUDING_AIAN;
