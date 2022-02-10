import { Workspace } from 'generated/fetch';

import { serverConfigStore } from 'app/utils/stores';

export const isUsingFreeTierBillingAccount = (
  workspace: Workspace
): boolean => {
  return (
    workspace.billingAccountName ===
    'billingAccounts/' + serverConfigStore.get().config.freeTierBillingAccountId
  );
};
