import * as React from 'react';

import {serverConfigStore} from 'app/utils/stores';
import {Workspace} from 'generated/fetch';

export const isUsingFreeTierBillingAccount = (workspace: Workspace): boolean => {
  return workspace.billingAccountName === 'billingAccounts/' + serverConfigStore.get().config.freeTierBillingAccountId
};
