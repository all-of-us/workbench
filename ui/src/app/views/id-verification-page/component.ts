import {Component, OnInit} from '@angular/core';
import {Router} from '@angular/router';
import {IdVerificationRequest, Profile, ProfileService} from 'generated';

function isBlank(s: string) {
  return (!s || /^\s*$/.test(s));
}

@Component({
  styleUrls: ['./component.css'],
  templateUrl: './component.html',
})

export class IdVerificationPageComponent implements OnInit {
  request: IdVerificationRequest;
  showError: boolean;
  errorMsg: string;
  constructor(
      private profileService: ProfileService,
      private router: Router
  ) {}

  ngOnInit(): void {
    this.errorMsg = '';
    this.showError = false;
    this.request = {
      firstName: '',
      lastName: '',
      streetLine1: '',
      streetLine2: '',
      city: '',
      state: '',
      zip: '',
      dob: '',
      documentType: '',
      documentNumber: ''
    };
    this.profileService.getMe().subscribe(
        (profile: Profile) => {
          this.request.firstName = profile.givenName;
          this.request.lastName = profile.familyName;
        });
  }

  submit(): void {
    const requiredFields =
        [this.request.firstName, this.request.lastName, this.request.streetLine1,
          this.request.streetLine2, this.request.city, this.request.state, this.request.zip,
          this.request.dob, this.request.documentNumber, this.request.documentType];
    if (requiredFields.some(isBlank)) {
      this.showError = true;
      this.errorMsg = 'All fields are required';
      return;
    }
    this.showError = false;
    let dobFormat = this.request.dob;
    const dobArr = dobFormat.split('/');
    // Date format YYYY-MM-DD as req in backend
    dobFormat = dobArr[2] + '-' + dobArr[1] + '-' + dobArr[0];
    this.request.dob = dobFormat;
    this.profileService.submitIdVerification(this.request).subscribe(() => {
      this.router.navigate(['profile']);
    }, error => {
      this.router.navigate(['profile']);
    });
  }

}

