import {userProfileStore} from 'app/utils/navigation';
import {Observable} from 'rxjs/Observable';
import {timer} from 'rxjs/observable/timer';
import 'rxjs/Rx';

import {Injectable} from '@angular/core';
import {ReplaySubject} from 'rxjs/ReplaySubject';

import {Profile} from 'generated';
import {ProfileService} from 'generated';


@Injectable()
export class ProfileStorageService {
  private static readonly RETRY_DELAY_MS = 500;

  private activeCall = false;
  private profile = new ReplaySubject<Profile>(1);
  public profile$ = this.profile.asObservable();

  constructor(private profileService: ProfileService) {
    this.reload();
  }

  /**
   * A retry policy which may be used in combination with Observable.retryWhen()
   * which retries conflict errors. This may be used with profile related
   * endpoints.
   */
  public static conflictRetryPolicy(): (errors: Observable<any>) => Observable<any> {
    let retries = 0;
    return (errs) => {
      return errs.flatMap((err) => {
        if (err.status !== 409) {
          throw err;
        }
        if (++retries >= 3) {
          throw err;
        }
        return timer(retries * ProfileStorageService.RETRY_DELAY_MS);
      });
    };
  }

  reload() {
    if (this.activeCall) {
      return;
    }
    this.activeCall = true;

    // Conflict 409s are not retries automatically. These have a likelihood to
    // occur on initial user login since we lazily initialize several things via
    // the /me profile endpoint.
    this.profileService.getMe()
      .retryWhen(ProfileStorageService.conflictRetryPolicy())
      .subscribe((profile) => {
        this.profile.next(profile);
        this.nextUserProfileStore(profile);
        this.activeCall = false;
      }, (err) => {
        this.activeCall = false;
      });
  }

  nextUserProfileStore(profile) {
    userProfileStore.next({
      profile,
      reload: () => this.reload(),
      updateCache: p => this.updateCache(p)
    });
  }

  updateCache(profile) {
    this.profile.next(profile);
    this.nextUserProfileStore(profile);
  }
}
