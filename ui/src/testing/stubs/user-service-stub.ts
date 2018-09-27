import {Injectable} from '@angular/core';
import {Observable} from 'rxjs/Observable';
import {User, UserResponse, WorkspaceAccessLevel} from '../../generated';

@Injectable()
export class UserServiceStub {
    constructor() {}

  user(searchTerm) {
    return new Observable<UserResponse>(observer => {
      setTimeout(() => {
        const roleNamePairs: User[] = [];
        roleNamePairs.push(<User>{
          familyName: 'Family',
          email: 'sampleuser4@fake-research-aou.org',
          givenName: 'GIVEN'
        });
        const userResponse: UserResponse = {
          users: roleNamePairs
        };
        observer.next(userResponse);
        observer.complete();
      }, 0);
    });
  }
}

