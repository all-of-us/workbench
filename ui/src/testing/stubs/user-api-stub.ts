import * as fp from 'lodash/fp';

import {
  User,
  UserApi,
  UserResponse,
  UserRole,
  WorkbenchListBillingAccountsResponse,
} from 'generated/fetch';

export class UserApiStub extends UserApi {
  existingUsers: UserRole[];
  constructor(existingUsers?: UserRole[]) {
    super(undefined);
    if (existingUsers) {
      this.existingUsers = existingUsers;
    }
  }

  listBillingAccounts(): Promise<WorkbenchListBillingAccountsResponse> {
    return new Promise<WorkbenchListBillingAccountsResponse>((resolve) => {
      resolve({
        billingAccounts: [
          {
            displayName: 'Free Tier',
            name: 'free-tier',
            freeTier: true,
            open: true,
          },
          {
            displayName: 'User Billing',
            name: 'user-billing',
            freeTier: false,
            open: true,
          },
        ],
      });
    });
  }
}
