import {Injectable} from '@angular/core';

declare const gapi: any;

@Injectable()
export class SignInService {
  constructor() {
    // console.log('In SignInService constructor.');
    // console.log(this.isAuth2Loaded());
    // this.initAuth2();
  }

  private isAuth2Loaded(): boolean {
    return typeof gapi.auth2 != 'undefined';
  }

  private isAuth2Initialized(): boolean {
    return gapi.auth2.getAuthInstance() != null;
  }

  private loadAuth2(): Promise<void> {
    if (this.isAuth2Loaded()) {
      return Promise.resolve(null);
    } else {
      return new Promise<void>(function(resolve) {
        gapi.load('auth2', () => resolve(null));
      });
    }
  }

  private initAuth2(): Promise<void> {
    if (this.isAuth2Initialized()) {
      return Promise.resolve(null);
    } else {
      return new Promise<void>(
        // Google doesn't know how to make real promises, so attempting to use the returned promise
        // leads to an infinite loop.
        (resolve) => gapi.auth2.init({
          client_id: '887440561153-pb9gmue2cbbs2gbn9nkr35g0ifpvb8g5.apps.googleusercontent.com',
          hosted_domain: 'pmi-ops.org'
      }).then(() => resolve(null)));
    }
  }

  getCurrentUser(): Promise<any> {
    return this.loadAuth2()
      .then(() => this.initAuth2())
      .then(() => gapi.auth2.getAuthInstance().currentUser);
  }

  isSignedIn(): Promise<boolean> {
    return this.getCurrentUser().then((user) => user.get().isSignedIn());
  }

  listenForUserDidChange(didChange): void {
    this.getCurrentUser().then((user) => user.listen(didChange));
  }

  // handleAuth2Initialized(): void {
  //   // gapi.auth2.getAuthInstance().currentUser.listen(this.handleUserDidChange.bind(this))
  //   // this.isSignedIn = gapi.auth2.getAuthInstance().currentUser.get().isSignedIn();
  //   // this.cdRef.detectChanges();
  // }

  handleAuth2Error(e: Error): void {
    console.error(e);
  }

  // handleUserDidChange(user: any): void {
  //   this.isSignedIn = user.isSignedIn();
  //   this.cdRef.detectChanges();
  // }

  // getLoggedInUser(): Promise<User> {
  //   return Promise.resolve(this.user);
  // }
  //
  // logIn(name: string, permission: PermissionLevel): Promise<User> {
  //   this.user = {id: 42, name: name, permission: permission};
  //   return this.getLoggedInUser();
  // }
  //
  // logOut(): Promise<void> {
  //   this.user = null;
  //   return Promise.resolve(null);
  // }
}
