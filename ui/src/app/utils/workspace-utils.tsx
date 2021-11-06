import {serverConfigStore} from 'app/utils/stores';
import {Workspace} from 'generated/fetch';
import * as React from 'react';

export const isUsingFreeTierBillingAccount = (workspace: Workspace): boolean => {
  return workspace.billingAccountName === 'billingAccounts/' + serverConfigStore.get().config.freeTierBillingAccountId
};
