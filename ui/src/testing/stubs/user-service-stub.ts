import {Injectable} from '@angular/core';
import {Observable} from 'rxjs/Observable';
import {User, UserResponse} from '../../generated';

@Injectable()
export class UserServiceStub {
    constructor() {}

  user(searchTerm) {
    return new Observable<UserResponse>(observer => {
      setTimeout(() => {
        const roleNamePairs: User[] = [];
        roleNamePairs.push(<User>{
          familyName: 'User4',
          email: 'sampleuser4@fake-research-aou.org',
          givenName: 'Sample'
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

