import {User, UserApi, UserResponse, UserRole, WorkbenchListBillingAccountsResponse} from 'generated/fetch';

import * as fp from 'lodash/fp';
import {stubNotImplementedError} from 'testing/stubs/stub-utils';

export class UserApiStub extends UserApi {
  existingUsers: UserRole[];
  constructor(existingUsers?: UserRole[]) {
    super(undefined, undefined, (..._: any[]) => { throw stubNotImplementedError; });
    if (existingUsers) {
      this.existingUsers = existingUsers;
    }
  }

  userSearch(accessTierShortName: string, searchTerm: string): Promise<UserResponse> {
    return new Promise<UserResponse>(resolve => {
      let usersToReturn: User[] = [];
      if (this.existingUsers) {
        usersToReturn = this.existingUsers.filter((userRole) => {
          return fp.includes(searchTerm, fp.values(userRole).join(' '));
        });
      } else {
        usersToReturn.push({
          familyName: 'User4',
          email: 'sampleuser4@fake-research-aou.org',
          givenName: 'Sample'
        } as User);
      }
      const userResponse: UserResponse = {
        users: usersToReturn
      };
      resolve(userResponse);
    });
  }

  listBillingAccounts(): Promise<WorkbenchListBillingAccountsResponse> {
    return new Promise<WorkbenchListBillingAccountsResponse>(resolve => {
      resolve({
        billingAccounts: [{
          displayName: 'Free Tier',
          name: 'free-tier',
          isFreeTier: true,
          isOpen: true}]
      });
    });
  }

}
