import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {ErrorHandlingService} from 'app/services/error-handling.service';

import {ErrorResponse, Profile, ProfileService} from 'generated';

@Component({
  styleUrls: ['./component.css'],
  templateUrl: './component.html',
})
export class ProfileEditComponent implements OnInit {
  profile: Profile;
  profileLoaded = false;
  errorText: string;
  constructor(
      private errorHandlingService: ErrorHandlingService,
      private profileService: ProfileService,
      private route: ActivatedRoute,
      private router: Router,
  ) {}

  ngOnInit(): void {
    this.errorText = null;
    this.profileService.getMe().subscribe(
      (profile: Profile) => {
        this.profile = profile;
        this.profileLoaded = true;
      });
  }

  submitChanges(): void {
    this.profileService.updateProfile(this.profile).subscribe(
      () => {
        this.router.navigate(['../'], {relativeTo : this.route});
      },
      error => {
        // if MailChimp throws an error, display to the user
        const response: ErrorResponse = ErrorHandlingService.convertAPIError(error);
        if (response.message && response.errorClassName.includes('MailchimpException')) {
          this.errorText = response.message;
        } else {
          this.errorText = '';
        }
      });
  }
}
