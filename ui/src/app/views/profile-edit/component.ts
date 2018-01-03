import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';

import {ErrorHandlingService} from 'app/services/error-handling.service';
import {SignInService} from 'app/services/sign-in.service';
import {Profile, ProfileService} from 'generated';

@Component({
  styleUrls: ['./component.css'],
  templateUrl: './component.html',
})
export class ProfileEditComponent implements OnInit {
  verifiedStatusIsLoaded: boolean;
  verifiedStatusIsValid: boolean;
  profile: Profile;
  profileLoaded = false;
  constructor(
      private errorHandlingService: ErrorHandlingService,
      private profileService: ProfileService,
      private route: ActivatedRoute,
      private router: Router,
      private signInService: SignInService,
  ) {}

  ngOnInit(): void {
    this.getVerifiedStatus();
  }

  getVerifiedStatus(): void {
    this.errorHandlingService.retryApi(this.profileService.getMe()).subscribe(
        (profile: Profile) => {
      this.verifiedStatusIsValid = profile.blockscoreVerificationIsValid;
      this.verifiedStatusIsLoaded = true;
      this.profile = profile;
      this.profileLoaded = true;
    });
  }

  deleteAccount(): void {
    this.profileService.deleteAccount().subscribe(() => {
      this.signInService.signOut();
    });
  }

  submitChanges(): void {
    this.errorHandlingService.retryApi(
        this.profileService.updateProfile(this.profile)).subscribe(() => {
        this.router.navigate(['../'], {relativeTo : this.route});
      }
    );
  }
}
