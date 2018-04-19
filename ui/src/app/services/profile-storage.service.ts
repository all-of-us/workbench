import 'rxjs/Rx';

import {Injectable} from '@angular/core';
import {ReplaySubject} from 'rxjs/ReplaySubject';

import {Profile} from 'generated';
import {ProfileService} from 'generated';


@Injectable()
export class ProfileStorageService {
  private activeCall = false;
  private profile = new ReplaySubject<Profile>(1);
  public profile$ = this.profile.asObservable();

  constructor(private profileService: ProfileService) {
    this.reload();
  }

  reload() {
    if (!this.activeCall) {
      this.activeCall = true;
      this.profileService.getMe().subscribe((profile) => {
        this.profile.next(profile);
        this.activeCall = false;
      });
    }
  }
}
