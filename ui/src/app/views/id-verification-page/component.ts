import {Component, OnInit} from '@angular/core';
import {Router} from '@angular/router';

import {IdVerificationRequest, ProfileService} from 'generated';

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
  documentType = '';
  documentNumber: string;

  constructor(
      private profileService: ProfileService,
      private router: Router,
  ) {}

  ngOnInit(): void {
    this.request = {
      firstName: '', lastName: '',
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
