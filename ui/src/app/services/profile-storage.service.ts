import {userProfileStore} from 'app/utils/navigation';
import 'rxjs/Rx';

import {Injectable} from '@angular/core';
import {ReplaySubject} from 'rxjs/ReplaySubject';

import {profileApi} from 'app/services/swagger-fetch-clients';
import {profileStore} from 'app/utils/stores';
import {Profile} from 'generated/fetch';


@Injectable()
export class ProfileStorageService {
  private activeCall = false;
  private profile = new ReplaySubject<Profile>(1);
  public profile$ = this.profile.asObservable();

  constructor() {
    this.reload();
  }

  reload() {
    if (this.activeCall) {
      return;
    }
    this.activeCall = true;

    profileApi().getMe()
      .then((profile) => {
        profileStore.set({...profileStore.get(), profile: profile});
        this.profile.next(profile);
        this.nextUserProfileStore(profile);
        this.activeCall = false;
      }).catch((err) => {
        profileStore.set({...profileStore.get(), profile: null});
        this.profile.error(err);
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
