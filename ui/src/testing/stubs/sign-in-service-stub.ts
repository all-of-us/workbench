import {Injectable} from '@angular/core';
import {ReplaySubject} from 'rxjs/ReplaySubject';


@Injectable()
export class SignInServiceStub {
  private isSignedIn = new ReplaySubject<boolean>(1);
  public isSignedIn$ = this.isSignedIn.asObservable();

  public currentAccessToken = null;
  public profileImage = null;

  public signIn(): void {
    this.isSignedIn.next(true);
  }

  public signOut(): void {
    this.isSignedIn.next(false);
  }
}
