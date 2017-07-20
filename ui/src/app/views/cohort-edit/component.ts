import {Component, OnInit} from '@angular/core';
import {Router} from '@angular/router';


import {CohortsService} from 'generated';
import {User} from 'app/models/user';
import {UserService} from 'app/services/user.service';
import {CohortEditService} from 'app/services/cohort-edit.service'

@Component({
  styleUrls: ['./component.css'],
  templateUrl: './component.html',
})
export class CohortEditComponent implements OnInit {
  user: User;

  cohortId: number;
  cohortName: string;
  cohortDescription: string = "Hello Cohort description";
  constructor(
      private router: Router,
      private userService: UserService,
      private cohortsService: CohortsService,
      private CohortEditService: CohortEditService
  ) {}

  ngOnInit(): void {
    this.userService.getLoggedInUser()
        .then(user => this.user = user);
  }

  testClick(): void {
    console.log(this.cohortName);
    console.log(this.cohortDescription);
    this.CohortEditService.add(this.cohortName, this.cohortDescription).then(
      cohorts => console.log(cohorts));
  }

}
