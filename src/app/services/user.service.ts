// A stub for user/login management, which simply trusts any login attempt
// and stores the current logged-in user in memory.
//
// A default user is logged in by default.

import {Injectable} from '@angular/core';

import {PermissionLevel} from 'app/models/permission-level';
import {User} from 'app/models/user';

@Injectable()
export class UserService {
  private user: User = {id: 1, name: 'All of Us User', permission: PermissionLevel.Public}

  getLoggedInUser(): Promise<User> {
    return Promise.resolve(this.user);
  }

  logIn(name: string, permission: PermissionLevel): Promise<User> {
    this.user = {id: 42, name: name, permission: permission};
    return this.getLoggedInUser();
  }

  logOut(): Promise<void> {
    this.user = null;
    return Promise.resolve(null);
  }
}
