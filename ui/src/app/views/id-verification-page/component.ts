import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {Comparator, StringFilter} from 'clarity-angular';

import {ErrorHandlingService} from 'app/services/error-handling.service';
import {SignInService} from 'app/services/sign-in.service';
import {ProfileService, IdVerificationRequest} from 'generated';

@Component({
  styleUrls: ['./component.css'],
  templateUrl: './component.html',
})
export class IdVerificationPageComponent implements OnInit {
  request: IdVerificationRequest;
  firstName: string;
  lastName: string;
  addressLine1: string;
  addressLine2: string;
  city: string;
  state: string;
  zip: string;
  dob: string;
  documentType: string = "";
  documentNumber: string;

  constructor(
      private errorHandlingService: ErrorHandlingService,
      private profileService: ProfileService,
      private router: Router,
      private route: ActivatedRoute,
      private signInService: SignInService,
  ) {}

  ngOnInit(): void {
    this.request = {
      firstName: "", lastName: "",
      streetLine1: '', streetLine2: '', city: '', state: '', zip: '',
      dob: '',
      documentType: '', documentNumber: ''
    };
  }

  submit(): void {
    this.profileService.submitIdVerification(this.request).subscribe(() => {
      this.router.navigate(['profile']);
    });
  }
}
