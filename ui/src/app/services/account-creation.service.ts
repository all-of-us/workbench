import 'rxjs/Rx';

import {Subject} from 'rxjs';

import {Injectable} from '@angular/core';

@Injectable()
export class AccountCreationService {

  private contactEmail = new Subject<string>();

  contactEmailUpdated$ = this.contactEmail.asObservable();

  updateEmail(email: string) {
    this.contactEmail.next(email);
  }

}
